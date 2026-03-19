package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.GeneralSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", value = Dist.CLIENT)
public class SoundEventHandler {

    private static final Map<ResourceLocation, Long> LAST_SENT = new ConcurrentHashMap<>(64);
    private static final long SERVER_DEDUP_MS = 200;
    private static final long CLEANUP_INTERVAL_MS = 15000;

    private static long lastCleanupTime = 0;

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!(event.getSound() instanceof SimpleSoundInstance sound)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;

        Map<String, GeneralSoundsConfig.SoundEntry> soundMap = GeneralSoundsConfig.getSounds();
        if (soundMap == null) return;

        ResourceLocation soundLoc = sound.getLocation();
        GeneralSoundsConfig.SoundEntry cfg = soundMap.get(soundLoc.toString());
        if (cfg == null || !cfg.enabled) return;

        long now = System.currentTimeMillis();
        Long lastSent = LAST_SENT.get(soundLoc);
        if (lastSent != null && (now - lastSent) < SERVER_DEDUP_MS) return;

        LAST_SENT.put(soundLoc, now);

        if ((now - lastCleanupTime) > CLEANUP_INTERVAL_MS) {
            LAST_SENT.entrySet().removeIf(e -> (now - e.getValue()) > CLEANUP_INTERVAL_MS);
            lastCleanupTime = now;
        }

        EZVCNetwork.INSTANCE.sendToServer(new GeneralSoundPacket(
                soundLoc,
                sound.getX(),
                sound.getY(),
                sound.getZ(),
                cfg.speed_multiplier,
                cfg.range_multiplier
        ));
    }
}