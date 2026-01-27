package com.armilp.ezvcsurvival.data;

import net.minecraft.util.math.Vec3d;

public class TimedSoundData {
    private final Vec3d position;
    private final long timestamp;
    private final double speedMultiplier;
    private final double rangeMultiplier;

    public TimedSoundData(Vec3d position, long timestamp, double speedMultiplier, double rangeMultiplier) {
        this.position = position;
        this.timestamp = timestamp;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
    }

    public Vec3d position() {
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
