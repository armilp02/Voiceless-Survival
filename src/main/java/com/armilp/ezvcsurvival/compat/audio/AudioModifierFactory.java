//package com.armilp.ezvcsurvival.compat.audio;
//
//import com.armilp.ezvcsurvival.compat.audio.modifier.NoOpAudioModifier;
//import com.armilp.ezvcsurvival.compat.audio.modifier.RealAudioModifier;
//import com.armilp.ezvcsurvival.util.IAudioModifier;
//import com.sonicether.soundphysics.SoundPhysicsMod;
//import net.minecraft.world.phys.Vec3;
//import net.minecraftforge.fml.ModList;
//
//public class AudioModifierFactory {
//
//    public static IAudioModifier createAudioModifier(double occlusion, String sound, Vec3 playerPosition, Vec3 senderPos) {
//        if (!ModList.get().isLoaded(SoundPhysicsMod.MODID)) {
//            return new NoOpAudioModifier();
//        }
//        return new RealAudioModifier(occlusion, sound, playerPosition, senderPos);
//    }
//}