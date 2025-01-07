package com.armilp.ezvcsurvival;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ForgeVoicechatPlugin
@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class Plugin implements VoicechatPlugin {

    private static final long DEBUG_COOLDOWN = 2000; // 2 segundos de cooldown
    private static final boolean DEBUG = false;

    private static final Map<UUID, Long> debugCooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> playerSoundLocations = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static VoicechatApi voicechatApi;

    @Override
    public String getPluginId() {
        return "ezvcsurvival";
    }

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
     * Obtiene la última ubicación de sonido registrada dentro de un rango.
     *
     * @param zombiePosition La posición desde donde buscar.
     * @param range          El rango en el que buscar.
     * @return La posición más cercana dentro del rango, o null si no hay ninguna.
     */
    public static BlockPos getLastSoundLocation(BlockPos zombiePosition, double range) {
        return playerSoundLocations.values().stream()
                .filter(pos -> zombiePosition.distSqr(pos) <= range * range) // Usamos distSqr(Vec3i)
                .min(Comparator.comparingDouble(pos -> zombiePosition.distSqr(pos)))
                .orElse(null);
    }

    /**
     * Maneja el evento de recepción de un paquete de micrófono.
     */
    public void onMicrophonePacket(MicrophonePacketEvent event) {
        MicrophonePacket packet = event.getPacket();
        if (packet == null) return;

        byte[] audioData = packet.getOpusEncodedData();
        if (audioData == null || audioData.length < 90) { // Ignorar paquetes pequeños
            if (DEBUG) {
                System.out.println("[DEBUG] Paquete ignorado: tamaño insuficiente (" +
                        (audioData != null ? audioData.length : 0) + ")");
            }
            return;
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

            playerSoundLocations.put(playerUUID, playerPosition);

            long currentTime = System.currentTimeMillis();
            long lastDebugTime = debugCooldowns.getOrDefault(playerUUID, 0L);

            if (DEBUG && currentTime - lastDebugTime >= DEBUG_COOLDOWN) {
                debugCooldowns.put(playerUUID, currentTime);
                System.out.println("[DEBUG] Posición del jugador registrada: " + playerPosition +
                        ", tamaño del paquete: " + audioData.length);
            }

            // Programar la eliminación después de 5 segundos
            scheduler.schedule(() -> playerSoundLocations.remove(playerUUID), 5, TimeUnit.SECONDS);
        }
    }
}
