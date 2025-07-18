package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.commands.SoundEffectCommand;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.GunshotData;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.modifier.custom.SilenceModifier;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashMap;
import java.util.Map;

public class GunFireListener {

    private static final List<GunshotData> gunshotPositions = new CopyOnWriteArrayList<>();
    private static final long EXPIRATION_TIME_MS = 5000;

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        ItemStack gunStack = event.getGunItemStack();
        IGun gun = IGun.getIGunOrNull(gunStack);

        LivingEntity shooter = event.getShooter();
        if (shooter != null && useSilenceSound(shooter, gunStack)) {
            return;
        }
        if (shooter == null) return;
        Vec3 shooterPos = shooter.position();

        GunTabType gunType = GunTabType.PISTOL;
        if (gun != null) {
            try {
                ResourceLocation gunId = gun.getGunId(gunStack);
                CommonGunIndex commonGunIndex = CommonGunIndexRegistry.getCommonGunIndex(gunId);
                if (commonGunIndex != null) {
                    String typeStr = commonGunIndex.getType();
                    gunType = GunTabType.valueOf(typeStr.toUpperCase());
                } else {
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
                    } else {
                        gunType = GunTabType.PISTOL;
                    }
                }
            } catch (Exception e) {
                gunType = GunTabType.PISTOL;
            }
        }

        if (shooter instanceof ServerPlayer serverPlayer) {
            SoundEffectCommand.applyEffect(serverPlayer);
        }
        gunshotPositions.add(new GunshotData(shooterPos, System.currentTimeMillis(), gunType));
    }

    private static boolean useSilenceSound(LivingEntity entity, ItemStack gunStack) {

        IGunOperator operator = IGunOperator.fromLivingEntity(entity);
        if (operator != null) {
            AttachmentCacheProperty cacheProperty = operator.getCacheProperty();
            if (cacheProperty != null) {
                Pair<Integer, Boolean> silence = cacheProperty.getCache(SilenceModifier.ID);
                if (silence != null && silence.right()) return true;
            }
        }
        // 2. Fallback manual por ID, ahora desde config
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun != null) {
            ResourceLocation gunId = gun.getGunId(gunStack);
            if (SoundConfig.isSilencedGun(gunId)) {
                return true;
            }
        }
        return false;
    }

    public static GunshotData getLastGunshotData() {
        long currentTime = System.currentTimeMillis();
        gunshotPositions.removeIf(record -> currentTime - record.timestamp > EXPIRATION_TIME_MS);
        return gunshotPositions.isEmpty() ? null : gunshotPositions.get(gunshotPositions.size() - 1);
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
