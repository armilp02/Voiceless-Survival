package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.GunshotData;
import com.tacz.guns.api.event.common.GunFireEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunFireListener {

    private static final List<GunshotData> gunshotPositions = new CopyOnWriteArrayList<>();
    private static final long EXPIRATION_TIME_MS = 5000;
    private static final boolean TACZ_LOADED = ModList.get().isLoaded("tacz");

    static {
        if (TACZ_LOADED) {
            MinecraftForge.EVENT_BUS.register(GunFireListener.class);
        }
    }

    @SubscribeEvent
    public static void onGunFire(Event event) {
        if (!TACZ_LOADED) return;

        if (event instanceof GunFireEvent gunFireEvent) {
            Vec3 shooterPos = gunFireEvent.getShooter().position();
            gunshotPositions.add(new GunshotData(shooterPos, System.currentTimeMillis()));
        }
    }

    public static Vec3 getLastGunshotPosition() {
        long currentTime = System.currentTimeMillis();
        gunshotPositions.removeIf(record -> currentTime - record.timestamp > EXPIRATION_TIME_MS);
        return gunshotPositions.isEmpty() ? null : gunshotPositions.get(gunshotPositions.size() - 1).position;
    }
}
