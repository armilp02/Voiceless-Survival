package com.armilp.ezvcsurvival.compat.audio;

import com.armilp.ezvcsurvival.compat.audio.modifier.IAudioModifier;
import com.armilp.ezvcsurvival.compat.audio.modifier.NoOpAudioModifier;
import com.armilp.ezvcsurvival.compat.audio.modifier.RealAudioModifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public class AudioModifierFactory {
    public static IAudioModifier createAudioModifier(double occlusion, String sound, Vec3 playerPosition, Vec3 senderPos) {
        // Si no está cargado el mod, usamos la versión no-op
        if (!ModList.get().isLoaded("sound_physics_remastered")) {
            return new NoOpAudioModifier();
        }
        // Si está cargado, usamos la implementación real
        return new RealAudioModifier(occlusion, sound, playerPosition, senderPos);
    }
}
