package com.armilp.ezvcsurvival.compat.audio.modifier;

import net.minecraft.world.phys.Vec3;

public class RealAudioModifier implements IAudioModifier {

    private final MobAudioModifier audioModifier;

    public RealAudioModifier(double occlusion, String sound, Vec3 playerPosition, Vec3 senderPos) {
        // Crea la instancia real del modificador de audio
        this.audioModifier = new MobAudioModifier(occlusion, sound);
        
        // Calcula el vector directo
        Vec3 directVector = playerPosition.subtract(senderPos);
        if (directVector.length() == 0) {
            directVector = new Vec3(1, 0, 0);
        }
        
        // Agrega las "airspaces"
        this.audioModifier.addDirectAirspace(directVector);
        this.audioModifier.addSharedAirspace(new Vec3(0, 1, 0), 5.0);
    }

    @Override
    public double computeModifiedRange(double baseRange) {
        return this.audioModifier.computeModifiedRange(baseRange);
    }
}
