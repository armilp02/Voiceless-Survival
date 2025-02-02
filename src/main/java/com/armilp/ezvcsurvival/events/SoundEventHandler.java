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
        // Check that the sound is an instance of SimpleSoundInstance (which has a position)
        if (!(event.getSound() instanceof SimpleSoundInstance sound)) {
            return;
        }

        // Extract the position of the sound
        double x = sound.getX();
        double y = sound.getY();
        double z = sound.getZ();
        Vec3 position = new Vec3(x, y, z);

        // Get the identifier of the sound
        ResourceLocation id = sound.getLocation();
        SoundEventTracker.registerSound(id, position);
    }
}
