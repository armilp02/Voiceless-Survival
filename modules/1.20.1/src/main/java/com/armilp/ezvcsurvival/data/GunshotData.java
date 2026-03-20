package com.armilp.ezvcsurvival.data;

import com.tacz.guns.api.item.GunTabType;
import net.minecraft.world.phys.Vec3;

public record GunshotData(Vec3 position, long timestamp, GunTabType gunType, boolean silenced) {}