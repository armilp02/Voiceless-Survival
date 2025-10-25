package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.data.SoundData;
import com.armilp.ezvcsurvival.events.ArmorEventHandler;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
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
public class Plugin implements VoicechatPlugin {

    private boolean DEBUG;
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
        this.DEBUG = VoiceConfig.DEBUG.get();
        if (DEBUG) {
            System.out.println("[DEBUG] VoiceChat Plugin initialized");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        if (DEBUG) {
            System.out.println("[DEBUG] Registered MicrophonePacketEvent");
        }
    }

    public static double getMaxAudioLevel(short[] samples) {
        double rms = 0D;

        for (int i = 0; i < samples.length; i++) {
            double sample = (double) samples[i] / (double) Short.MAX_VALUE;
            rms += sample * sample;
        }

        int sampleCount = samples.length / 2;
        rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

        double db;
        if (rms > 0D) {
            db = Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
        } else {
            db = -127D;
        }

        return db;
    }

    @Nullable
    public static BlockPos getLastSoundLocation(BlockPos mobPosition, double range, double minDb) {
        return playerSoundLocations.values().stream()
                .filter(data -> data.getAudioLevelDb() >= minDb) // cada mob compara con su threshold
                .filter(data -> mobPosition.distSqr(data.getPosition()) <= range * range)
                .min(Comparator.comparingDouble(data -> mobPosition.distSqr(data.getPosition())))
                .map(SoundData::getPosition)
                .orElse(null);
    }

    public void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.getPlayer() == null) return;

        if (sender.getPlayer().getPlayer() instanceof ServerPlayer player
                && (player.isCreative() || player.isSpectator())) return;

        OpusDecoder localDecoder = decoder;
        if (localDecoder == null || localDecoder.isClosed()) {
            localDecoder = voicechatApi.createDecoder();
            decoder = localDecoder;
        }
        if (localDecoder == null) {
            return;
        }
        localDecoder.resetState();

        byte[] opusEncodedData = event.getPacket().getOpusEncodedData();
        short[] decoded;
        try {
            decoded = localDecoder.decode(opusEncodedData);
        } catch (Exception e) {
            return;
        }

        double audioLevel = getMaxAudioLevel(decoded);

        UUID playerUUID = sender.getPlayer().getUuid();
        Position voicechatPosition = sender.getPlayer().getPosition();

        Vec3 senderVec = new Vec3(
                voicechatPosition.getX(),
                voicechatPosition.getY(),
                voicechatPosition.getZ()
        );
        BlockPos playerPosition = new BlockPos(
                (int) Math.floor(senderVec.x),
                (int) Math.floor(senderVec.y),
                (int) Math.floor(senderVec.z)
        );

        boolean isWhispering = event.getPacket().isWhispering();
        double whisperRangeMultiplier = VoiceConfig.WHISPER_RANGE_MULTIPLIER.get();
        double whisperSpeedMultiplier = VoiceConfig.WHISPER_SPEED_MULTIPLIER.get();
        double thunderRangeMultiplier = VoiceConfig.THUNDER_RANGE_MULTIPLIER.get();
        double sneakingRangeMultiplier = VoiceConfig.SNEAKING_RANGE_MULTIPLIER.get();

        List<String> allIds = new ArrayList<>(EntityVoiceConfig.getAllEntityIds());

        long currentTime = System.currentTimeMillis();

        for (String id : allIds) {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(id);
            if (cfg == null) cfg = EntityVoiceConfig.getAnimal(id);
            if (cfg == null || !cfg.enabled) continue;
            double threshold = cfg.threshold;
            double detectionRange = cfg.range;
            double speed = cfg.speed;

            if (isWhispering) {
                detectionRange *= whisperRangeMultiplier;
                speed *= whisperSpeedMultiplier;
            }

            if (sender.getPlayer().getPlayer() instanceof ServerPlayer p) {
                if (p.isCrouching()) detectionRange *= sneakingRangeMultiplier;
                if (p.level().isRaining() || p.level().isThundering())
                    detectionRange *= thunderRangeMultiplier;
                double[] armorMult = ArmorEventHandler.getArmorMultipliers(p);
                detectionRange *= armorMult[1];
                speed *= armorMult[0];
            }

            // --- DISABLED FOR NOW ---
            // IAudioModifier audioModifier = AudioModifierFactory.createAudioModifier(
            //         0.5, "voicechat", senderVec, senderVec
            // );
            // double modifiedRange = audioModifier.computeModifiedRange(detectionRange);
            double modifiedRange = detectionRange;

            double distance = senderVec.distanceTo(new Vec3(playerPosition.getX(), playerPosition.getY(), playerPosition.getZ()));
            double distanceVolume = 1.0 - Math.min(distance, modifiedRange) / modifiedRange;

            if (audioLevel >= threshold && distanceVolume > 0.0) {
                BlockPos precisePos = new BlockPos(
                        (int) Math.floor(senderVec.x),
                        (int) Math.floor(senderVec.y),
                        (int) Math.floor(senderVec.z)
                );
                playerSoundLocations.put(
                        playerUUID,
                        new SoundData(precisePos, audioLevel)
                );
                if (DEBUG) {
                    System.out.println("[DEBUG] " + id + " detects sound! " +
                            "Threshold: " + threshold + " dB | " +
                            "AudioLevel: " + audioLevel + " dB | " +
                            "Range: " + detectionRange + " | " +
                            "Speed: " + speed + " | " +
                            "Position: " + precisePos);
                }

                if (DEBUG) {
                    System.out.println("[DEBUG] Intensity/range too low for " + id + ": "
                            + audioLevel + " dB | " + distanceVolume);
                }
            }
        }
        scheduler.schedule(() -> playerSoundLocations.remove(playerUUID), 5, TimeUnit.SECONDS);
    }
}
