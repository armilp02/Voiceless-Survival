package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.audio.AudioModifierFactory;
import com.armilp.ezvcsurvival.audio.modifier.IAudioModifier;
import com.armilp.ezvcsurvival.data.SoundData;
import com.armilp.ezvcsurvival.events.ArmorEventHandler;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
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
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<UUID, SoundData> playerSoundLocations = new ConcurrentHashMap<>();

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
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.getPlayer() == null) {
            return;
        }
        if (sender.getPlayer().getPlayer() instanceof ServerPlayer player && player.isCreative() && player.isSpectator()) {
            return;
        }
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
        UUID playerUUID = sender.getPlayer().getUuid();
        de.maxhenkel.voicechat.api.Position voicechatPosition = sender.getPlayer().getPosition();
        BlockPos playerPosition = new BlockPos(
                (int) Math.floor(voicechatPosition.getX()),
                (int) Math.floor(voicechatPosition.getY()),
                (int) Math.floor(voicechatPosition.getZ())
        );
        BlockPos senderPosition = playerPosition;
        double distance = Math.sqrt(playerPosition.distSqr(senderPosition));
        double perceivedIntensity = audioLevel - 20 * Math.log10(distance + 1);
        if (DEBUG) {
            System.out.println("[DEBUG] Perceived Intensity: " + perceivedIntensity + " dB");
        }

        boolean isWhispering = event.getPacket().isWhispering();
        double whisperRangeMultiplier = VoiceConfig.WHISPER_RANGE_MULTIPLIER.get();
        double whisperSpeedMultiplier = VoiceConfig.WHISPER_SPEED_MULTIPLIER.get();
        double thunderRangeMultiplier = VoiceConfig.THUNDER_RANGE_MULTIPLIER.get();
        double sneakingRangeMultiplier = VoiceConfig.SNEAKING_RANGE_MULTIPLIER.get();

        List<String> mobIds = getConfiguredMobIds();
        List<String> animalIds = getConfiguredAnimalIds();
        for (String animalId : animalIds) {
            double threshold = getActivationThreshold(animalId);
            double detectionRange = getDetectionRange(animalId);
            double speed = getSpeed(animalId);

            if (isWhispering) {
                detectionRange *= whisperRangeMultiplier;
                speed *= whisperSpeedMultiplier;
                if (sender.getPlayer().getPlayer() instanceof ServerPlayer player) {
                    if (player.isCrouching()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }
                    if (player.level.isRaining() || player.level.isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                    double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
                    detectionRange *= armorMult[1];
                    speed *= armorMult[0];
                }
            } else {
                if (sender.getPlayer().getPlayer() instanceof ServerPlayer player) {
                    if (player.isCrouching()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }
                    if (player.level.isRaining() || player.level.isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                    double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
                    detectionRange *= armorMult[1];
                    speed *= armorMult[0];
                }
            }

            BlockPos senderPos = new BlockPos(
                    (int) Math.floor(voicechatPosition.getX()),
                    (int) Math.floor(voicechatPosition.getY()),
                    (int) Math.floor(voicechatPosition.getZ())
            );

            Vec3 senderVec = new Vec3(voicechatPosition.getX(), voicechatPosition.getY(), voicechatPosition.getZ());
            Vec3 playerVec = new Vec3(voicechatPosition.getX(), voicechatPosition.getY(), voicechatPosition.getZ());

            IAudioModifier audioModifier = AudioModifierFactory.createAudioModifier(0.5, "voicechat", playerVec, senderVec);
            detectionRange = audioModifier.computeModifiedRange(detectionRange);

            double distanceSq = playerPosition.distSqr(senderPos);
            double perceivedIntensityAnimal = audioLevel - 20 * Math.log10(Math.sqrt(distanceSq) + 1);
            if (DEBUG) {
                System.out.println("[DEBUG] Perceived Intensity for " + animalId + ": " + perceivedIntensityAnimal + " dB at distance " + Math.sqrt(distanceSq));
            }
            if (perceivedIntensityAnimal < threshold) {
                if (DEBUG) {
                    System.out.println("[DEBUG] Intensity too low for " + animalId + ": " + perceivedIntensityAnimal + " dB");
                }
                continue;
            }
            if (distanceSq <= detectionRange * detectionRange) {
                playerSoundLocations.put(playerUUID, new SoundData(playerPosition, detectionRange, speed));
                if (DEBUG) {
                    System.out.println("[DEBUG] " + animalId + " detects sound at range " + detectionRange + " with speed " + speed + " from position " + playerPosition);
                }
            }
        }
        for (String mobId : mobIds) {
            double threshold = getActivationThreshold(mobId);
            double detectionRange = getDetectionRange(mobId);
            double speed = getSpeed(mobId);

            if (isWhispering) {
                detectionRange *= whisperRangeMultiplier;
                speed *= whisperSpeedMultiplier;
                if (sender.getPlayer().getPlayer() instanceof ServerPlayer player) {
                    if (player.isCrouching()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }
                    if (player.level.isRaining() || player.level.isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                    double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
                    detectionRange *= armorMult[1];
                    speed *= armorMult[0];
                }
            } else {
                if (sender.getPlayer().getPlayer() instanceof ServerPlayer player) {
                    if (player.isCrouching()) {
                        detectionRange *= sneakingRangeMultiplier;
                    }
                    if (player.level.isRaining() || player.level.isThundering()) {
                        detectionRange *= thunderRangeMultiplier;
                    }
                    double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
                    detectionRange *= armorMult[1];
                    speed *= armorMult[0];
                }
            }

            BlockPos senderPos = new BlockPos(
                    (int) Math.floor(voicechatPosition.getX()),
                    (int) Math.floor(voicechatPosition.getY()),
                    (int) Math.floor(voicechatPosition.getZ())
            );
            Vec3 senderVec = new Vec3(voicechatPosition.getX(), voicechatPosition.getY(), voicechatPosition.getZ());
            Vec3 playerVec = new Vec3(voicechatPosition.getX(), voicechatPosition.getY(), voicechatPosition.getZ());

            IAudioModifier audioModifier = AudioModifierFactory.createAudioModifier(0.5, "voicechat", playerVec, senderVec);
            detectionRange = audioModifier.computeModifiedRange(detectionRange);

            double distanceSq = playerPosition.distSqr(senderPos);
            double perceivedIntensityMob = audioLevel - 20 * Math.log10(Math.sqrt(distanceSq) + 1);
            if (DEBUG) {
                System.out.println("[DEBUG] Perceived Intensity for " + mobId + ": " + perceivedIntensityMob + " dB at distance " + Math.sqrt(distanceSq));
            }
            if (perceivedIntensityMob < threshold) {
                if (DEBUG) {
                    System.out.println("[DEBUG] Intensity too low for " + mobId + ": " + perceivedIntensityMob + " dB");
                }
                continue;
            }

            if (distanceSq <= detectionRange * detectionRange) {
                playerSoundLocations.put(playerUUID, new SoundData(playerPosition, detectionRange, speed));
                if (DEBUG) {
                    System.out.println("[DEBUG] " + mobId + " detects sound at modified range " + detectionRange + " with modified speed " + speed + " from position " + playerPosition);
                }
            }
        }
        scheduler.schedule(() -> playerSoundLocations.remove(playerUUID), 5, TimeUnit.SECONDS);
    }

    private List<String> getConfiguredMobIds() {
        Map<String, Map<String, Double>> mobConfigs = VoiceConfig.getMobVoiceConfigs();
        return new ArrayList<>(mobConfigs.keySet());
    }

    private List<String> getConfiguredAnimalIds() {
        Map<String, Map<String, Double>> mobConfigs = VoiceConfig.getAnimalVoiceConfigs();
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
}
