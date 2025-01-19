package com.armilp.ezvcsurvival;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.common.Mod;
import com.armilp.ezvcsurvival.config.VoiceConfig;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ForgeVoicechatPlugin
@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class Plugin implements VoicechatPlugin {

    private static final boolean DEBUG = true;
    private static final Map<UUID, SoundData> playerSoundLocations = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static VoicechatApi voicechatApi;

    @Override
    public String getPluginId() {
        return "ezvcsurvival";
    }

    @Nullable
    private OpusDecoder decoder;

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
        if (DEBUG) {
            System.out.println("[DEBUG] VoiceChat Plugin initialized");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        if (DEBUG) {
            System.out.println("[DEBUG] Registro del evento MicrophonePacketEvent");
        }
    }

    public static double calculateAudioLevel(short[] samples) {
        double rms = 0D;

        for (short sample : samples) {
            double normalizedSample = (double) sample / (double) Short.MAX_VALUE;
            rms += normalizedSample * normalizedSample;
        }

        int sampleCount = samples.length;
        rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

        if (rms > 0D) {
            return Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
        } else {
            return -127D;
        }
    }

    public static BlockPos getLastSoundLocation(BlockPos zombiePosition, double range) {
        return playerSoundLocations.values().stream()
                .filter(data -> zombiePosition.distSqr(data.getPosition()) <= data.getRange() * data.getRange())
                .min(Comparator.comparingDouble(data -> zombiePosition.distSqr(data.getPosition())))
                .map(SoundData::getPosition)
                .orElse(null);
    }

    public static double getLastSoundSpeed(BlockPos zombiePosition, double range) {
        return playerSoundLocations.values().stream()
                .filter(data -> zombiePosition.distSqr(data.getPosition()) <= data.getRange() * data.getRange())
                .min(Comparator.comparingDouble(data -> zombiePosition.distSqr(data.getPosition())))
                .map(SoundData::getSpeed)
                .orElse(1.0);
    }

    public void onMicrophonePacket(MicrophonePacketEvent event) {
        if (decoder == null || decoder.isClosed()) {
            decoder = voicechatApi.createDecoder();
        }

        decoder.resetState();
        byte[] opusEncodedData = event.getPacket().getOpusEncodedData();
        short[] decoded;

        try {
            decoded = decoder.decode(opusEncodedData);
        } catch (Exception e) {
            return;
        }

        double audioLevel = calculateAudioLevel(decoded);

        VoicechatConnection sender = event.getSenderConnection();
        if (sender != null) {
            UUID playerUUID = sender.getPlayer().getUuid();
            de.maxhenkel.voicechat.api.Position voicechatPosition = sender.getPlayer().getPosition();
            BlockPos playerPosition = new BlockPos(
                    (int) Math.floor(voicechatPosition.getX()),
                    (int) Math.floor(voicechatPosition.getY()),
                    (int) Math.floor(voicechatPosition.getZ())
            );

            boolean isWhispering = event.getPacket().isWhispering();

            double whisperRangeMultiplier = VoiceConfig.WHISPER_RANGE_MULTIPLIER.get();
            double whisperSpeedMultiplier = VoiceConfig.WHISPER_SPEED_MULTIPLIER.get();
            double thunderRangeMultiplier = VoiceConfig.THUNDER_RANGE_MULTIPLIER.get();
            double sneakingRangeMultiplier = VoiceConfig.SNEAKING_RANGE_MULTIPLIER.get();

            List<String> mobIds = getConfiguredMobIds();

            for (String mobId : mobIds) {
                double threshold = getActivationThreshold(mobId);
                double detectionRange = getDetectionRange(mobId);
                double speed = getSpeed(mobId);

                if (isWhispering) {
                    detectionRange *= whisperRangeMultiplier;
                    speed *= whisperSpeedMultiplier;

                    Object minecraftPlayer = sender.getPlayer().getPlayer();
                    if (minecraftPlayer instanceof net.minecraft.server.level.ServerPlayer player) {
                        if (player.isCrouching()) {
                            detectionRange *= sneakingRangeMultiplier;
                        }

                        if (player.level.isRaining() || player.level.isThundering()) {
                            detectionRange *= thunderRangeMultiplier;
                        }
                    }
                } else {
                    Object minecraftPlayer = sender.getPlayer().getPlayer();
                    if (minecraftPlayer instanceof net.minecraft.server.level.ServerPlayer player) {
                        if (player.isCrouching()) {
                            detectionRange *= sneakingRangeMultiplier;
                        }
                        if (player.level.isRaining() || player.level.isThundering()) {
                            detectionRange *= thunderRangeMultiplier;
                        }
                    }
                }

                BlockPos senderPosition = new BlockPos(
                        (int) Math.floor(sender.getPlayer().getPosition().getX()),
                        (int) Math.floor(sender.getPlayer().getPosition().getY()),
                        (int) Math.floor(sender.getPlayer().getPosition().getZ())
                );

                double distance = Math.sqrt(playerPosition.distSqr(senderPosition));
                double perceivedIntensity = audioLevel - 20 * Math.log10(distance + 1);

                if (DEBUG) {
                    System.out.println("[DEBUG] Perceived Intensity for " + mobId + ": " + perceivedIntensity + " dB at distance " + distance);
                }

                if (perceivedIntensity < threshold) {
                    if (DEBUG) {
                        System.out.println("[DEBUG] Intensity too low for " + mobId + ": " + perceivedIntensity + " dB");
                    }
                    continue;
                }

                if (playerPosition.distSqr(senderPosition) <= detectionRange * detectionRange) {
                    playerSoundLocations.put(playerUUID, new SoundData(playerPosition, detectionRange, speed));

                    if (DEBUG) {
                        System.out.println("[DEBUG] " + mobId + " detects sound at range " + detectionRange + " with speed " + speed + " from position " + playerPosition);
                    }
                }
            }
        }
    }


    private List<String> getConfiguredMobIds() {
        Map<String, Map<String, Double>> mobConfigs = VoiceConfig.getMobVoiceConfigs();
        return new ArrayList<>(mobConfigs.keySet());
    }

    private double getActivationThreshold(String mobId) {
        Map<String, Double> mobConfig = VoiceConfig.getMobVoiceConfigs().get(mobId);
        if (mobConfig != null && mobConfig.containsKey("threshold")) {
            return mobConfig.get("threshold");
        }
        return -40.0;
    }

    private double getDetectionRange(String mobId) {
        Map<String, Double> mobConfig = VoiceConfig.getMobVoiceConfigs().get(mobId);
        if (mobConfig != null && mobConfig.containsKey("range")) {
            return mobConfig.get("range");
        }
        return 16.0;
    }

    private double getSpeed(String mobId) {
        Map<String, Double> mobConfig = VoiceConfig.getMobVoiceConfigs().get(mobId);
        if (mobConfig != null && mobConfig.containsKey("speed")) {
            return mobConfig.get("speed");
        }
        return 1.0;
    }

    private static class SoundData {
        private final BlockPos position;
        private final double range;
        private final double speed;

        public SoundData(BlockPos position, double range, double speed) {
            this.position = position;
            this.range = range;
            this.speed = speed;
        }

        public BlockPos getPosition() {
            return position;
        }

        public double getRange() {
            return range;
        }

        public double getSpeed() {
            return speed;
        }
    }
}
