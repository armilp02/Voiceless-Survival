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
        if ("pointblank".equals(soundLocation.getNamespace())) {

            EZVCNetwork.INSTANCE.sendToServer(new SoundPlayedPacket(soundLocation.toString()));
        }
    }
}
