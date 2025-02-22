package com.armilp.ezvcsurvival.events;

import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", value = Dist.CLIENT)
public class SoundEventHandler {

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!(event.getSound() instanceof SimpleSound)) {
            return;
        }

        SimpleSound sound = (SimpleSound) event.getSound();

        Vector3d position = new Vector3d(sound.getX(), sound.getY(), sound.getZ());
        ResourceLocation soundId = sound.getLocation();
        SoundEventTracker.registerSound(soundId, position);
    }
}