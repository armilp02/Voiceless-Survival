package com.armilp.ezvcsurvival.client;

import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.SoundPlayedPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", value = Dist.CLIENT)
public class ClientSoundEventHandler {

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        ResourceLocation soundLocation = event.getSound().getLocation();
        String namespace = soundLocation.getNamespace();
        String path = soundLocation.getPath();

        if ("pointblank".equals(namespace)
                && !path.contains("_s") && !path.contains("_magin")
                && !path.contains("_magout") && !path.contains("_reload")
                && !path.contains("_draw") && !path.contains("draw")
                && !path.contains("_open") && !path.contains("_close")
                && !path.contains("hit") && !path.contains("_slide")
                && !path.contains("added") && !path.contains("removed")
                && !path.contains("_unload") && !path.contains("_load")) {
            EZVCNetwork.INSTANCE.sendToServer(new SoundPlayedPacket(soundLocation.toString()));
        }
    }
}
