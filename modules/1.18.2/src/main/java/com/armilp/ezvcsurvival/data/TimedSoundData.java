package com.armilp.ezvcsurvival.data;

import net.minecraft.world.phys.Vec3;

public record TimedSoundData(Vec3 position, long timestamp, double speedMultiplier, double rangeMultiplier) {
}
