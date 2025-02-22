package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.SoundConfig;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import java.util.List;

public class SoundEventHandler {

    private static final List<String> bannedSubstrings = List.of(
            "_s", "_magin", "_magout", "_reload", "_draw", "draw",
            "_open", "_close", "hit", "_slide", "added", "removed", "unload", "load"
    );

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        ResourceLocation soundId = event.getSound().getLocation();
        Vec3 position = Vec3.ZERO;

        if (event.getSound() instanceof SimpleSoundInstance simpleSound) {
            position = new Vec3(simpleSound.getX(), simpleSound.getY(), simpleSound.getZ());
            SoundEventTracker.registerSound(soundId, position);
        }

        // Procesamos sonidos del namespace "pointblank"
        if ("pointblank".equals(soundId.getNamespace())) {
            String soundString = soundId.toString();
            for (String banned : bannedSubstrings) {
                if (soundString.contains(banned)) {
                    return;
                }
            }
            SoundConfig.registerPointblankSound(soundString);
        }
    }

}
