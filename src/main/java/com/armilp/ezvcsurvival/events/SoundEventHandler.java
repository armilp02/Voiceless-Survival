package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.GeneralSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) {
            return;
        }

        var soundMap = GeneralSoundsConfig.getSounds();
        if (soundMap == null) {
            return;
        }

        GeneralSoundsConfig.SoundEntry cfg = soundMap.get(sound.getLocation().toString());
        if (cfg == null || !cfg.enabled) {
            return;
        }

        EZVCNetwork.INSTANCE.sendToServer(new GeneralSoundPacket(
                sound.getLocation(),
                sound.getX(),
                sound.getY(),
                sound.getZ(),
                cfg.speed_multiplier,
                cfg.range_multiplier
        ));
    }
}
