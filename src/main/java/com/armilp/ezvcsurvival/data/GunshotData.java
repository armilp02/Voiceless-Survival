package com.armilp.ezvcsurvival.data;

import com.tacz.guns.api.item.GunTabType;
import net.minecraft.world.phys.Vec3;

public class GunshotData {
    public final Vec3 position;
    public final long timestamp;
    public final GunTabType gunType; // Nuevo campo

    public GunshotData(Vec3 position, long timestamp, GunTabType gunType) {
        this.position = position;
        this.timestamp = timestamp;
        this.gunType = gunType;
    }
}
