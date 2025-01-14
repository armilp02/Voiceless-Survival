package com.armilp.ezvcsurvival;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.core.BlockPos;
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

    private static final boolean DEBUG = false;

    private static final Map<UUID, BlockPos> playerSoundLocations = new ConcurrentHashMap<>();
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
                .filter(pos -> zombiePosition.distSqr(pos) <= range * range)
                .min(Comparator.comparingDouble(zombiePosition::distSqr))
                .orElse(null);
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
            if (DEBUG) {
                System.out.println("[DEBUG] Error al decodificar el paquete: " + e.getMessage());
            }
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

            List<String> mobIds = getConfiguredMobIds();

            for (String mobId : mobIds) {
                double threshold = getActivationThreshold(mobId);

                if (audioLevel < threshold) {
                    if (DEBUG) {
                        System.out.println("[DEBUG] Nivel de audio demasiado bajo para " + mobId + ": " + audioLevel + " dB");
                    }
                    continue;
                }

                playerSoundLocations.put(playerUUID, playerPosition);

                if (DEBUG) {
                    System.out.println("[DEBUG] Nivel de audio aceptado para " + mobId + ": " + audioLevel + " dB en posición " + playerPosition);
                }
            }

            scheduler.schedule(() -> playerSoundLocations.remove(playerUUID), 5, TimeUnit.SECONDS);
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
        System.out.println("[VoiceConfig] Mob ID not found or no threshold set: " + mobId);
        return -40.0;
    }
}
