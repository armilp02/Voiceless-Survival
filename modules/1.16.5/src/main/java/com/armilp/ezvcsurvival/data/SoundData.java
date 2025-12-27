package com.armilp.ezvcsurvival.data;

import net.minecraft.util.math.BlockPos;

public class SoundData {
    private final BlockPos position;
    private final double audioLevelDb;

    public SoundData(BlockPos position, double audioLevelDb) {
        this.position = position;
        this.audioLevelDb = audioLevelDb;
    }

    public BlockPos getPosition() {
        return position;
    }

    public double getAudioLevelDb() {
        return audioLevelDb;
    }
}