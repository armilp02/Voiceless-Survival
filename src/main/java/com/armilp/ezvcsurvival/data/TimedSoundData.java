package com.armilp.ezvcsurvival.data;


import net.minecraft.util.math.vector.Vector3d;

public class TimedSoundData {
        public final Vector3d position;
        public final long timestamp;

        public TimedSoundData(Vector3d position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }