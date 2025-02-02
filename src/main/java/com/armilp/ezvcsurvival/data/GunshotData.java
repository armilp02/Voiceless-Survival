package com.armilp.ezvcsurvival.data;

import net.minecraft.world.phys.Vec3;

public class GunshotData {
    public final Vec3 position;
    public final long timestamp;

        public GunshotData(Vec3 position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }