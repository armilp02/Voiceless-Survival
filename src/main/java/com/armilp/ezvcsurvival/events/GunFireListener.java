package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.GunshotData;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class GunFireListener {

    private static final List<GunshotData> gunshotPositions = new CopyOnWriteArrayList<>();
    private static final long EXPIRATION_TIME_MS = 5000;

    static {
        MinecraftForge.EVENT_BUS.register(GunFireListener.class);
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        ItemStack gunStack = event.getGunItemStack();
        IGun gun = IGun.getIGunOrNull(gunStack);

        LivingEntity shooter = event.getShooter();
        if (shooter != null && useSilenceSound(shooter)) {
            return;
        }

        assert shooter != null;
        Vec3 shooterPos = shooter.position();
        GunTabType gunType = GunTabType.PISTOL;
        if (gun != null) {
            ResourceLocation gunId = gun.getGunId(gunStack);
            String gunIdStr = gunId.toString().toLowerCase();
            if (gunIdStr.contains("sniper")) {
                gunType = GunTabType.SNIPER;
            } else if (gunIdStr.contains("rifle")) {
                gunType = GunTabType.RIFLE;
            } else if (gunIdStr.contains("shotgun")) {
                gunType = GunTabType.SHOTGUN;
            } else if (gunIdStr.contains("smg")) {
                gunType = GunTabType.SMG;
            } else if (gunIdStr.contains("rpg")) {
                gunType = GunTabType.RPG;
            } else if (gunIdStr.contains("mg")) {
                gunType = GunTabType.MG;
            }
        }

        gunshotPositions.add(new GunshotData(shooterPos, System.currentTimeMillis(), gunType));
    }

    private static boolean useSilenceSound(LivingEntity player) {
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
        if (cacheProperty != null) {
            Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
            return silence.right();
        }
        return false;
    }

    public static GunshotData getLastGunshotData() {
        long currentTime = System.currentTimeMillis();
        gunshotPositions.removeIf(record -> currentTime - record.timestamp > EXPIRATION_TIME_MS);
        return gunshotPositions.isEmpty() ? null : gunshotPositions.get(gunshotPositions.size() - 1);
    }
}
