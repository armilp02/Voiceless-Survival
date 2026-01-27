package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.GeneralSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class SoundEventHandler {

    private static final Map<ResourceLocation, Long> SOUND_COOLDOWNS = new ConcurrentHashMap<ResourceLocation, Long>();
    private static final long EXPLOSION_COOLDOWN_MS = 300;
    private static final long DEFAULT_COOLDOWN_MS = 50;

    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL_MS = 10000;
    private static final int MAX_COOLDOWN_ENTRIES = 150;

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        ISound soundInstance = event.getSound();
        if (soundInstance == null) {
            return;
        }

        // En 1.12.2 usamos PositionedSoundRecord en lugar de SimpleSound
        if (!(soundInstance instanceof PositionedSoundRecord)) {
            return;
        }

        PositionedSoundRecord sound = (PositionedSoundRecord) soundInstance;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getConnection() == null || mc.player == null) {
            return;
        }

        Map<String, GeneralSoundsConfig.SoundEntry> soundMap = GeneralSoundsConfig.getSounds();
        if (soundMap == null) {
            return;
        }

        ResourceLocation soundLoc = sound.getSoundLocation();
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

        EZVCNetwork.INSTANCE.sendToServer(new GeneralSoundPacket(
                soundLoc,
                sound.getXPosF(),
                sound.getYPosF(),
                sound.getZPosF(),
                cfg.speed_multiplier,
                cfg.range_multiplier
        ));
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

        Iterator<Map.Entry<ResourceLocation, Long>> iterator = SOUND_COOLDOWNS.entrySet().iterator();
        long threshold = EXPLOSION_COOLDOWN_MS * 4;

        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, Long> entry = iterator.next();
            if ((currentTime - entry.getValue()) > threshold) {
                iterator.remove();
            }
        }
    }
}