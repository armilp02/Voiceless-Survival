package com.armilp.ezvcsurvival.data;


import net.minecraft.util.math.vector.Vector3d;

public class GunshotData {
    public final Vector3d position;
    public final long timestamp;

        public GunshotData(Vector3d position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }