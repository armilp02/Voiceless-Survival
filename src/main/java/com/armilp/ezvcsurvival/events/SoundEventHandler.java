package com.armilp.ezvcsurvival.events;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

public class SoundEventHandler {

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (!(event.getSound() instanceof SimpleSoundInstance sound)) {
            return;
        }
        Vec3 position = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        ResourceLocation soundId = sound.getLocation();
        SoundEventTracker.registerSound(soundId, position);
    }
}
