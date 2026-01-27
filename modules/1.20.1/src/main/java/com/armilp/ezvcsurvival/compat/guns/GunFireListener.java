package com.armilp.ezvcsurvival.compat.guns;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.commands.SoundEffectCommand;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
                String gunIdStr = gunId.toString().toLowerCase();

                if (SoundConfig.isDebugEnabled()) {
                    EZVCSurvival.LOGGER.info("[GunFire] Gun fired: {}", gunId);
                }

                CommonGunIndex commonGunIndex = CommonGunIndexRegistry.getCommonGunIndex(gunId);

                if (commonGunIndex != null) {
                    String typeStr = commonGunIndex.getType();
                    try {
                        gunType = GunTabType.valueOf(typeStr.toUpperCase());
                        if (SoundConfig.isDebugEnabled()) {
                            EZVCSurvival.LOGGER.info("[GunFire] ✓ Type from registry: {}", gunType);
                        }
                    } catch (IllegalArgumentException e) {
                        EZVCSurvival.LOGGER.warn("[GunFire] Invalid gun type in index: {}", typeStr);
                        gunType = inferGunTypeFromId(gunIdStr);
                    }
                } else {
                    if (SoundConfig.isDebugEnabled()) {
                        EZVCSurvival.LOGGER.info("[GunFire] No index found, inferring from ID...");
                    }
                    gunType = inferGunTypeFromId(gunIdStr);
                }

                if (SoundConfig.isDebugEnabled()) {
                    String gunTypeLower = gunType.name().toLowerCase();
                    double speedMult = SoundConfig.getSpeedMultiplier(gunTypeLower);
                    double rangeMult = SoundConfig.getRangeMultiplier(gunTypeLower);

                    EZVCSurvival.LOGGER.info("[GunFire] Final: {} -> Type: {}, Speed: {}x, Range: {}x",
                            gunId, gunType, speedMult, rangeMult);
                }

            } catch (Exception e) {
                EZVCSurvival.LOGGER.error("[GunFire] Error determining gun type for {}", gun.getGunId(gunStack), e);
                gunType = GunTabType.PISTOL;
            }
        }

        if (shooter instanceof ServerPlayer serverPlayer) {
            SoundEffectCommand.applyEffect(serverPlayer);
        }

        gunshotPositions.add(new GunshotData(shooterPos, System.currentTimeMillis(), gunType));
    }

    private static GunTabType inferGunTypeFromId(String gunIdStr) {
        GunTabType result;

        if (gunIdStr.contains("sniper") || gunIdStr.contains("awp") || gunIdStr.contains("barrett")) {
            result = GunTabType.SNIPER;
        } else if (gunIdStr.contains("rifle") || gunIdStr.contains("ak") || gunIdStr.contains("m4") ||
                gunIdStr.contains("scar") || gunIdStr.contains("hk416")) {
            result = GunTabType.RIFLE;
        } else if (gunIdStr.contains("shotgun") || gunIdStr.contains("spas") || gunIdStr.contains("m870")) {
            result = GunTabType.SHOTGUN;
        } else if (gunIdStr.contains("smg") || gunIdStr.contains("mp5") || gunIdStr.contains("ump") ||
                gunIdStr.contains("vector") || gunIdStr.contains("uzi")) {
            result = GunTabType.SMG;
        } else if (gunIdStr.contains("rpg") || gunIdStr.contains("rocket") || gunIdStr.contains("launcher")) {
            result = GunTabType.RPG;
        } else if (gunIdStr.contains("mg") || gunIdStr.contains("lmg") || gunIdStr.contains("m249") ||
                gunIdStr.contains("minigun")) {
            result = GunTabType.MG;
        } else {
            result = GunTabType.PISTOL;
        }

        if (SoundConfig.isDebugEnabled()) {
            EZVCSurvival.LOGGER.info("[GunFire] Inferred type from '{}': {}", gunIdStr, result);
        }
        return result;
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
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (gun != null) {
            ResourceLocation gunId = gun.getGunId(gunStack);
            return SoundConfig.isSilencedGun(gunId);
        }
        return false;
    }

    public static GunshotData getLastGunshotData() {
        long currentTime = System.currentTimeMillis();
        gunshotPositions.removeIf(record -> currentTime - record.timestamp() > EXPIRATION_TIME_MS);
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