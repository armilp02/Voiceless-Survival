package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.data.GunshotData;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.compat.tacz.GunFireListener;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.config.SoundConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

public class ReactToGunfireGoal extends Goal {
    private final Mob mob;
    private final double baseSpeed;
    private final double baseRange;

    public static Vec3 lastPrioritySoundPos = null;
    public static long lastPrioritySoundTimestamp = 0;
    private static final long PRIORITY_SOUND_DURATION_MS = 3000;

    public ReactToGunfireGoal(Mob mob, double speed, double range) {
        this.mob = mob;
        this.baseSpeed = speed;
        this.baseRange = range;
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) {
            return false;
        }

        if (lastPrioritySoundPos != null && System.currentTimeMillis() - lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        checkForPrioritySounds();

        Vec3 mobCenterPos = mob.position();

        if (lastPrioritySoundPos != null) {
            double priorityRange = baseRange * 1.5;
            if (mob.level().isRaining() || mob.level().isThundering()) {
                priorityRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }
            if (mobCenterPos.distanceTo(lastPrioritySoundPos) <= priorityRange) {
                return true;
            }
        }

        if (lastPrioritySoundPos == null) {
            double effectiveRange = baseRange;
            String effectiveGunType = null;

            GunshotData gunshotData = GunFireListener.getLastGunshotData();
            if (gunshotData != null) {
                effectiveGunType = gunshotData.gunType.name().toLowerCase();
            }

            if (effectiveGunType != null) {
                double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                effectiveRange = baseRange * rangeMultiplier;
            }

            if (mob.level().isRaining() || mob.level().isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }

            return gunshotData != null && mobCenterPos.distanceTo(gunshotData.position) <= effectiveRange;
        }

        return false;
    }

    @Override
    public void start() {
        updateNavigation();
    }

    @Override
    public void tick() {
        updateNavigation();
    }

    private void checkForPrioritySounds() {
        for (SoundGroupData priority : SoundConfig.getPriorityGroups()) {
            for (String soundStr : priority.sounds) {
                ResourceLocation soundLoc = toLocation(soundStr);
                Vec3 priorityPos = SoundEventTracker.getLastPlayedPositionForSound(soundLoc);
                if (priorityPos != null) {
                    lastPrioritySoundPos = priorityPos;
                    lastPrioritySoundTimestamp = System.currentTimeMillis();
                    break;
                }
            }
            if (lastPrioritySoundPos != null) break;
        }
    }

    private void updateNavigation() {
        if (lastPrioritySoundPos != null && System.currentTimeMillis() - lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        checkForPrioritySounds();

        Vec3 currentPos = mob.position();
        Vec3 target = null;
        double effectiveRange = baseRange;
        double effectiveSpeed = baseSpeed;

        if (lastPrioritySoundPos != null) {
            effectiveRange = baseRange * 1.5;
            effectiveSpeed = baseSpeed * 1.3;

            if (mob.level().isRaining() || mob.level().isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }

            if (currentPos.distanceTo(lastPrioritySoundPos) <= effectiveRange) {
                if (mob instanceof Monster) {
                    target = grounded(lastPrioritySoundPos);
                } else {
                    Vec3 directionAway = currentPos.subtract(lastPrioritySoundPos).normalize();
                    target = grounded(currentPos.add(directionAway.scale(effectiveRange)));
                }

                if (target != null) {
                    mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
                    if (currentPos.distanceTo(lastPrioritySoundPos) < 2.0) {
                        lastPrioritySoundPos = null;
                    }
                }
                return;
            }
        }

        if (lastPrioritySoundPos == null) {
            String effectiveGunType = null;

            GunshotData gunshotData = GunFireListener.getLastGunshotData();
            if (gunshotData != null) {
                effectiveGunType = gunshotData.gunType.name().toLowerCase();
            }

            if (effectiveGunType != null) {
                double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                double speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
                effectiveRange = baseRange * rangeMultiplier;
                effectiveSpeed = baseSpeed * speedMultiplier;
            }

            if (mob.level().isRaining() || mob.level().isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }

            if (mob instanceof Monster) {
                if (gunshotData != null && currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
                    target = grounded(gunshotData.position);
                }

                if (target != null) {
                    mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
                }
            } else {
                if (gunshotData != null && currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
                    Vec3 directionAway = currentPos.subtract(gunshotData.position).normalize();
                    Vec3 fleeTarget = grounded(currentPos.add(directionAway.scale(effectiveRange)));
                    mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, effectiveSpeed);
                }
            }
        }
    }

    private Vec3 grounded(Vec3 desiredXZ) {
        BlockPos base = BlockPos.containing(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    private ResourceLocation toLocation(String soundStr) {
        if (soundStr.contains(":")) {
            return new ResourceLocation(soundStr);
        }
        return new ResourceLocation("minecraft", soundStr);
    }
}
