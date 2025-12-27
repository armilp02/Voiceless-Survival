package com.armilp.ezvcsurvival.data;

import net.minecraft.util.math.vector.Vector3d;

public class GunshotData {
    public final Vector3d position;
    public final long timestamp;
    public final String gunType;

    public GunshotData(Vector3d position, long timestamp, String gunType) {
        this.position = position;
        this.timestamp = timestamp;
        this.gunType = gunType != null ? gunType : "";
    }

    public GunshotData(Vector3d position, long timestamp) {
        this(position, timestamp, "");
    }
}