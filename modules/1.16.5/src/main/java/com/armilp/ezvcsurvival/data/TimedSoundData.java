package com.armilp.ezvcsurvival.data;

import net.minecraft.util.math.vector.Vector3d;

public class TimedSoundData {
    private final Vector3d position;
    private final long timestamp;
    private final double speedMultiplier;
    private final double rangeMultiplier;

    public TimedSoundData(Vector3d position, long timestamp, double speedMultiplier, double rangeMultiplier) {
        this.position = position;
        this.timestamp = timestamp;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
    }

    public Vector3d position() {
        return position;
    }

    public long timestamp() {
        return timestamp;
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    public double rangeMultiplier() {
        return rangeMultiplier;
    }
}