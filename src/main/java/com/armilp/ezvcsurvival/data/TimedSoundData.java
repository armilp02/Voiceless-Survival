package com.armilp.ezvcsurvival.data;

import net.minecraft.world.phys.Vec3;

public class TimedSoundData {
        public final Vec3 position;
        public final long timestamp;

        public TimedSoundData(Vec3 position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }