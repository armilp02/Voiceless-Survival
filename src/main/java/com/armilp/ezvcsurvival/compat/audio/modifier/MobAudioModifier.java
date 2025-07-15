package com.armilp.ezvcsurvival.compat.audio.modifier;

import com.sonicether.soundphysics.ReflectedAudio;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class MobAudioModifier {

    private final ReflectedAudio reflectedAudio;
    private final double baseIntensity;
    private final List<Double> sharedIntensityContributions = new ArrayList<>();

    public MobAudioModifier(double occlusion, String sound) {
        this.reflectedAudio = new ReflectedAudio(occlusion, sound);
        this.baseIntensity = computeBaseIntensity(occlusion);
    }

    private double computeBaseIntensity(double occlusion) {
        double randomFactor = 0.95 + Math.random() * 0.1;
        double intensity = (1.0 / (occlusion + 0.1)) * randomFactor;
        return Math.min(Math.max(intensity, 1.0), 2.5);
    }

    private double computeSharedIntensity(double totalRayDistance) {
        double intensity = 1.0 / (totalRayDistance + 1.0);
        double scalingFactor = 0.3;
        return intensity * scalingFactor;
    }

    public void addDirectAirspace(Vec3 direction) {
        this.reflectedAudio.addDirectAirspace(direction);
    }

    public void addSharedAirspace(Vec3 direction, double totalRayDistance) {
        this.reflectedAudio.addSharedAirspace(direction, totalRayDistance);
        sharedIntensityContributions.add(computeSharedIntensity(totalRayDistance));
    }

    /**
     * Calcula el factor de intensidad total.
     * Se utiliza un escalado logarítmico para amortiguar las contribuciones compartidas y se limita el valor máximo,
     * evitando así que el multiplicador se dispare sin nerfear el efecto base.
     */
    public double getIntensityFactor() {
        double totalSharedIntensity = sharedIntensityContributions.stream().mapToDouble(Double::doubleValue).sum();
        // Se aplica una función logarítmica para evitar que las contribuciones acumuladas crezcan linealmente
        double dampenedSharedIntensity = Math.log1p(totalSharedIntensity);
        double intensityFactor = baseIntensity + dampenedSharedIntensity;
        // Se limita el factor de intensidad a un máximo razonable (por ejemplo, 3.5)
        return Math.min(intensityFactor, 3.5);
    }

    public double computeModifiedRange(double baseRange) {
        return baseRange * getIntensityFactor();
    }
}
