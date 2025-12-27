//package com.armilp.ezvcsurvival.compat.audio;
//
//import com.armilp.ezvcsurvival.compat.audio.modifier.NoOpAudioModifier;
//import com.armilp.ezvcsurvival.compat.audio.modifier.SoundPhysicsAudioModifier;
//import com.armilp.ezvcsurvival.config.VoiceConfig;
//import com.armilp.ezvcsurvival.util.IAudioModifier;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.Vec3;
//
//public class AudioModifierFactory {
//
//    private static Boolean soundPhysicsLoaded = null;
//
//    private static boolean isSoundPhysicsLoaded() {
//        if (soundPhysicsLoaded == null) {
//            try {
//                Class.forName("com.sonicether.soundphysics.SoundPhysicsMod");
//                soundPhysicsLoaded = true;
//            } catch (ClassNotFoundException e) {
//                soundPhysicsLoaded = false;
//            }
//        }
//        return soundPhysicsLoaded;
//    }
//
//    public static IAudioModifier createAudioModifier(double occlusion, Level level, Vec3 playerPos, Vec3 senderPos) {
//        try {
//            if (!VoiceConfig.SOUND_PHYSICS_INTEGRATION_ENABLED.get()) {
//                return new NoOpAudioModifier();
//            }
//
//            if (!isSoundPhysicsLoaded() || level == null || playerPos == null || senderPos == null) {
//                return new NoOpAudioModifier();
//            }
//
//            IAudioModifier modifier = new SoundPhysicsAudioModifier(occlusion, level, playerPos, senderPos);
//
//            if (!modifier.isValid()) {
//                return new NoOpAudioModifier();
//            }
//
//            return modifier;
//
//        } catch (Throwable e) {
//            return new NoOpAudioModifier();
//        }
//    }
//}