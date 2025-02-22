package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.SoundConfig;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", value = Dist.CLIENT)
public class SoundEventHandler {

    private static final List<String> bannedSubstrings = new ArrayList<>(
            List.of("_s", "_magin", "_magout", "_reload", "_draw", "draw",
                    "_open", "_close", "hit", "_slide", "added", "removed", "_unload", "_load"));


    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!(event.getSound() instanceof SimpleSoundInstance sound)) {
            return;
        }

        Vec3 position = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        ResourceLocation soundId = sound.getLocation();
        SoundEventTracker.registerSound(soundId, position);

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
