package com.armilp.ezvcsurvival.events;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
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

        Vec3 position = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        ResourceLocation soundId = sound.getLocation();
        SoundEventTracker.registerSound(soundId, position);
    }
}
