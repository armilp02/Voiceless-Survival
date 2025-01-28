package com.armilp.ezvcsurvival.data;


import net.minecraft.util.math.BlockPos;

public  class SoundData {
        private final BlockPos position;
        private final double range;
        private final double speed;

        public SoundData(BlockPos position, double range, double speed) {
            this.position = position;
            this.range = range;
            this.speed = speed;
        }

        public BlockPos getPosition() {
            return position;
        }

        public double getRange() {
            return range;
        }

        public double getSpeed() {
            return speed;
        }
    }