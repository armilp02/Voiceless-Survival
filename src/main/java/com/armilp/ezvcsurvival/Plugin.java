package com.armilp.ezvcsurvival;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ForgeVoicechatPlugin
@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class Plugin implements VoicechatPlugin {


    private static final double MIN_ACTIVATION_THRESHOLD = -40.0;
    private static final boolean DEBUG = true;

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

    /**
     * Calcula el nivel de audio en decibelios (dB) a partir de una señal PCM.
     *
     * @param samples La señal PCM en formato short.
     * @return El nivel de audio en dB.
     */
    public static double calculateAudioLevel(short[] samples) {
        double rms = 0D; // Amplitud RMS (root mean square)

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

    /**
     * Obtiene la última ubicación de sonido registrada dentro de un rango.
     *
     * @param zombiePosition La posición desde donde buscar.
     * @param range          El rango en el que buscar.
     * @return La posición más cercana dentro del rango, o null si no hay ninguna.
     */
    public static BlockPos getLastSoundLocation(BlockPos zombiePosition, double range) {
        return playerSoundLocations.values().stream()
                .filter(pos -> zombiePosition.distSqr(pos) <= range * range) // Usamos distSqr(Vec3i)
                .min(Comparator.comparingDouble(zombiePosition::distSqr))
                .orElse(null);
    }

    /**
     * Maneja el evento de recepción de un paquete de micrófono.
     */
    /**
     * Maneja el evento de recepción de un paquete de micrófono.
     */
    public void onMicrophonePacket(MicrophonePacketEvent event) {
        if (decoder == null || decoder.isClosed()) {
            decoder = voicechatApi.createDecoder(); // Asegurarse de que el decodificador está disponible
        }

        decoder.resetState(); // Reiniciar el estado del decodificador
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

        if (audioLevel < MIN_ACTIVATION_THRESHOLD) {
            if (DEBUG) {
                System.out.println("[DEBUG] Nivel de audio demasiado bajo: " + audioLevel + " dB");
            }
            return; // Ignorar si el nivel de audio está por debajo del umbral
        }

        VoicechatConnection sender = event.getSenderConnection();
        if (sender != null) {
            UUID playerUUID = sender.getPlayer().getUuid();
            de.maxhenkel.voicechat.api.Position voicechatPosition = sender.getPlayer().getPosition();
            BlockPos playerPosition = new BlockPos(
                    (int) Math.floor(voicechatPosition.getX()),
                    (int) Math.floor(voicechatPosition.getY()),
                    (int) Math.floor(voicechatPosition.getZ())
            );

            // Registrar la posición del jugador para otros sistemas
            playerSoundLocations.put(playerUUID, playerPosition);

            if (DEBUG) {
                System.out.println("[DEBUG] Nivel de audio aceptado: " + audioLevel + " dB en posición " + playerPosition);
            }

            // Programar eliminación después de 5 segundos
            scheduler.schedule(() -> playerSoundLocations.remove(playerUUID), 5, TimeUnit.SECONDS);
        }
    }
}
