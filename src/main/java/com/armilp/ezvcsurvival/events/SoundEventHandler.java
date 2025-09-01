package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.GeneralSoundPacket;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", value = Dist.CLIENT)
public class SoundEventHandler {

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!(event.getSound() instanceof SimpleSoundInstance sound)) {
            return;
        }

        ResourceLocation soundId = sound.getLocation();
        if (soundId == null) return;

        if (net.minecraft.client.Minecraft.getInstance().getConnection() == null
                || net.minecraft.client.Minecraft.getInstance().player == null) {
            return;
        }

        GeneralSoundsConfig.SoundEntry cfg = null;
        try {
            var map = GeneralSoundsConfig.getSounds();
            if (map != null) cfg = map.get(soundId.toString());
        } catch (Exception ignored) {}
        if (cfg == null || !cfg.enabled) return;

        EZVCNetwork.INSTANCE.sendToServer(new GeneralSoundPacket(
                soundId,
                sound.getX(),
                sound.getY(),
                sound.getZ(),
                cfg.speed_multiplier,
                cfg.range_multiplier
        ));
    }
}
