package com.armilp.ezvcsurvival.data;

import net.minecraft.world.phys.Vec3;

public class TimedSoundData {
    public final Vec3 position;
    public final long timestamp;
    public final double speedMultiplier;
    public final double rangeMultiplier;

    public TimedSoundData(Vec3 position, long timestamp, double speedMultiplier, double rangeMultiplier) {
        this.position = position;
        this.timestamp = timestamp;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
    }

    public TimedSoundData(Vec3 position, long timestamp) {
        this(position, timestamp, 1.0, 1.0);
    }
}
