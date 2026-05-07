package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.GeneralSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventHandler {

    private static final Map<Identifier, Long> SOUND_COOLDOWNS = new ConcurrentHashMap<>(128);
    private static final long EXPLOSION_COOLDOWN_MS = 300;
    private static final long DEFAULT_COOLDOWN_MS = 50;

    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 10000;
    private static final int MAX_COOLDOWN_ENTRIES = 150;

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!(event.getSound() instanceof SimpleSoundInstance sound)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) {
            return;
        }

        var soundMap = GeneralSoundsConfig.getSounds();
        if (soundMap == null) {
            return;
        }

        Identifier soundLoc = sound.getIdentifier();
        String soundId = soundLoc.toString();

        GeneralSoundsConfig.SoundEntry cfg = soundMap.get(soundId);
        if (cfg == null || !cfg.enabled) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (cfg.is_priority) {
            Long lastTime = SOUND_COOLDOWNS.get(soundLoc);
            if (lastTime != null && (currentTime - lastTime) < 50) {
                return;
            }
            SOUND_COOLDOWNS.put(soundLoc, currentTime);
        } else if (shouldApplyCooldown(soundId, cfg.is_priority)) {
            Long lastTime = SOUND_COOLDOWNS.get(soundLoc);
            long cooldownDuration = getCooldownDuration(soundId, cfg.is_priority);

            if (lastTime != null && (currentTime - lastTime) < cooldownDuration) {
                return;
            }

            SOUND_COOLDOWNS.put(soundLoc, currentTime);
        }

        if ((currentTime - lastCleanupTime) > CLEANUP_INTERVAL_MS) {
            cleanupCooldowns(currentTime);
            lastCleanupTime = currentTime;
        }

        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(
                    new GeneralSoundPacket(
                            sound.getIdentifier(),
                            sound.getX(),
                            sound.getY(),
                            sound.getZ(),
                            cfg.speed_multiplier,
                            cfg.range_multiplier
                    )
            ));
        }
    }

    private static boolean shouldApplyCooldown(String soundId, boolean isPriority) {
        if (isPriority) {
            return true;
        }

        if (soundId.contains("explode") || soundId.contains("explosion")) {
            return true;
        }

        return soundId.contains("tnt");
    }

    private static long getCooldownDuration(String soundId, boolean isPriority) {
        if (isPriority) {
            return 50;
        }

        if (soundId.contains("explode") || soundId.contains("explosion") || soundId.contains("tnt")) {
            return EXPLOSION_COOLDOWN_MS;
        }

        return DEFAULT_COOLDOWN_MS;
    }

    private static void cleanupCooldowns(long currentTime) {
        if (SOUND_COOLDOWNS.size() > MAX_COOLDOWN_ENTRIES) {
            SOUND_COOLDOWNS.clear();
            return;
        }

        Iterator<Map.Entry<Identifier, Long>> iterator = SOUND_COOLDOWNS.entrySet().iterator();
        long threshold = EXPLOSION_COOLDOWN_MS * 4;

        while (iterator.hasNext()) {
            Map.Entry<Identifier, Long> entry = iterator.next();
            if ((currentTime - entry.getValue()) > threshold) {
                iterator.remove();
            }
        }
    }
}