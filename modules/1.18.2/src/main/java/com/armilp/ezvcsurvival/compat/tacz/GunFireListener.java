package com.armilp.ezvcsurvival.compat.tacz;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.GunshotData;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class GunFireListener {

    private static final long EXPIRATION_TIME_MS = 5000;
    private static final AtomicReference<GunshotData> lastShot = new AtomicReference<>(null);

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        LivingEntity shooter = event.getShooter();
        if (shooter == null) return;

        ItemStack gunStack = event.getGunItemStack();
        IGun gun = IGun.getIGunOrNull(gunStack);

        boolean silenced = isSilenced(shooter, gunStack);

        if (silenced && !SoundConfig.isSilencerModifiersEnabled()) return;

        Vec3 shooterPos = shooter.position();
        GunTabType gunType = GunTabType.PISTOL;

        if (gun != null) {
            try {
                ResourceLocation gunId = gun.getGunId(gunStack);
                String gunIdStr = gunId.toString().toLowerCase();

                CommonGunIndex commonGunIndex = CommonGunIndexRegistry.getCommonGunIndex(gunId);

                if (commonGunIndex != null) {
                    String typeStr = commonGunIndex.getType();
                    try {
                        gunType = GunTabType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        gunType = inferGunTypeFromId(gunIdStr);
                    }
                } else {
                    gunType = inferGunTypeFromId(gunIdStr);
                }
            } catch (Exception e) {
                gunType = GunTabType.PISTOL;
            }
        }

        lastShot.set(new GunshotData(shooterPos, System.currentTimeMillis(), gunType, silenced));
    }

    private static GunTabType inferGunTypeFromId(String gunIdStr) {
        if (gunIdStr.contains("sniper") || gunIdStr.contains("awp") || gunIdStr.contains("barrett")) {
            return GunTabType.SNIPER;
        } else if (gunIdStr.contains("rifle") || gunIdStr.contains("ak") || gunIdStr.contains("m4") ||
                gunIdStr.contains("scar") || gunIdStr.contains("hk416")) {
            return GunTabType.RIFLE;
        } else if (gunIdStr.contains("shotgun") || gunIdStr.contains("spas") || gunIdStr.contains("m870")) {
            return GunTabType.SHOTGUN;
        } else if (gunIdStr.contains("smg") || gunIdStr.contains("mp5") || gunIdStr.contains("ump") ||
                gunIdStr.contains("vector") || gunIdStr.contains("uzi")) {
            return GunTabType.SMG;
        } else if (gunIdStr.contains("rpg") || gunIdStr.contains("rocket") || gunIdStr.contains("launcher")) {
            return GunTabType.RPG;
        } else if (gunIdStr.contains("mg") || gunIdStr.contains("lmg") || gunIdStr.contains("m249") ||
                gunIdStr.contains("minigun")) {
            return GunTabType.MG;
        }
        return GunTabType.PISTOL;
    }

    private static boolean isSilenced(LivingEntity entity, ItemStack gunStack) {
        IGunOperator operator = IGunOperator.fromLivingEntity(entity);
        if (operator != null) {
            AttachmentCacheProperty cacheProperty = operator.getCacheProperty();
            if (cacheProperty != null) {
                Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
                if (silence != null && silence.right()) return true;
            }
        }
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun != null) {
            return SoundConfig.isSilencedGun(gun.getGunId(gunStack));
        }
        return false;
    }

    public static GunshotData getLastGunshotData() {
        GunshotData data = lastShot.get();
        if (data == null) return null;
        if (System.currentTimeMillis() - data.timestamp() > EXPIRATION_TIME_MS) {
            lastShot.compareAndSet(data, null);
            return null;
        }
        return data;
    }

    public static class CommonGunIndexRegistry {
        private static final Map<ResourceLocation, CommonGunIndex> registry = new HashMap<>();

        public static CommonGunIndex getCommonGunIndex(ResourceLocation id) {
            return registry.get(id);
        }

        public static void registerCommonGunIndex(ResourceLocation id, CommonGunIndex index) {
            registry.put(id, index);
        }
    }
}