package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.GunshotData;
import com.tacz.guns.api.event.common.GunFireEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class GunFireListener {

    // Lista de disparos recientes con su tiempo de registro
    private static final List<GunshotData> gunshotPositions = new CopyOnWriteArrayList<>();
    private static final long EXPIRATION_TIME_MS = 5000; // 5 segundos

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        Vec3 shooterPos = event.getShooter().position();
        gunshotPositions.add(new GunshotData(shooterPos, System.currentTimeMillis()));
    }

    public static Vec3 getLastGunshotPosition() {
        long currentTime = System.currentTimeMillis();

        // Elimina los registros viejos
        gunshotPositions.removeIf(record -> currentTime - record.timestamp > EXPIRATION_TIME_MS);

        return gunshotPositions.isEmpty() ? null : gunshotPositions.get(gunshotPositions.size() - 1).position;
    }

}
