package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.compat.tacz.GunFireListener;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.GunshotData;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class ReactToGunfireGoal extends Goal {
    private final Mob mob;
    private final double baseSpeed;
    private final double baseRange;

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

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null &&
                System.currentTimeMillis() - ReactToGeneralSoundGoal.lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
            ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
        }

        checkForPrioritySounds();

        Vec3 mobCenterPos = mob.position();

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null) {
            double priorityRange = baseRange * 1.5;
            if (mob.level.isRaining() || mob.level.isThundering()) {
                priorityRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }
            if (mobCenterPos.distanceTo(ReactToGeneralSoundGoal.lastPrioritySoundPos) <= priorityRange) {
                return true;
            }
        }

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos == null) {
            double effectiveRange = baseRange;
            String effectiveGunType = null;

            GunshotData gunshotData = GunFireListener.getLastGunshotData();
            if (gunshotData != null) {
                effectiveGunType = gunshotData.gunType().name().toLowerCase();
            }

            if (effectiveGunType != null) {
                double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                effectiveRange = baseRange * rangeMultiplier;
            }

            if (mob.level.isRaining() || mob.level.isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }

            return gunshotData != null && mobCenterPos.distanceTo(gunshotData.position()) <= effectiveRange;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) {
            return false;
        }

        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        updateNavigation();
    }

    @Override
    public void tick() {
        updateNavigation();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    private void checkForPrioritySounds() {
        for (SoundGroupData priority : SoundConfig.getPriorityGroups()) {
            for (String soundStr : priority.sounds()) {
                ResourceLocation soundLoc = toLocation(soundStr);
                Vec3 priorityPos = SoundEventTracker.getLastPlayedPositionForSound(soundLoc);
                if (priorityPos != null) {
                    ReactToGeneralSoundGoal.setPrioritySound(priorityPos);
                    return;
                }
            }
        }

        GunshotData gunshotData = GunFireListener.getLastGunshotData();
        if (gunshotData != null) {
            String gunType = gunshotData.gunType().name().toLowerCase();
            if (isPriorityGunType(gunType)) {
                ReactToGeneralSoundGoal.setPrioritySound(gunshotData.position());
            }
        }
    }

    private boolean isPriorityGunType(String gunType) {
        double rangeMultiplier = SoundConfig.getRangeMultiplier(gunType);
        return rangeMultiplier >= 6.0;
    }

    private void updateNavigation() {
        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null &&
                System.currentTimeMillis() - ReactToGeneralSoundGoal.lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
            ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
        }

        checkForPrioritySounds();

        Vec3 currentPos = mob.position();
        Vec3 target = null;
        double effectiveRange = baseRange;
        double effectiveSpeed = baseSpeed;

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null) {
            effectiveRange = baseRange * 1.5;
            effectiveSpeed = baseSpeed * 1.3;

            if (mob.level.isRaining() || mob.level.isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }

            if (currentPos.distanceTo(ReactToGeneralSoundGoal.lastPrioritySoundPos) <= effectiveRange) {
                if (mob instanceof Monster) {
                    target = findAccessiblePosition(ReactToGeneralSoundGoal.lastPrioritySoundPos);
                } else {
                    Vec3 directionAway = currentPos.subtract(ReactToGeneralSoundGoal.lastPrioritySoundPos).normalize();
                    target = findAccessiblePosition(currentPos.add(directionAway.scale(effectiveRange)));
                }

                if (target != null) {
                    mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
                    if (currentPos.distanceTo(ReactToGeneralSoundGoal.lastPrioritySoundPos) < 2.0) {
                        ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
                    }
                }
                return;
            }
        }

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos == null) {
            String effectiveGunType = null;

            GunshotData gunshotData = GunFireListener.getLastGunshotData();
            if (gunshotData != null) {
                effectiveGunType = gunshotData.gunType().name().toLowerCase();
            }

            if (effectiveGunType != null) {
                double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                double speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
                effectiveRange = baseRange * rangeMultiplier;
                effectiveSpeed = baseSpeed * speedMultiplier;
            }

            if (mob.level.isRaining() || mob.level.isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }

            if (mob instanceof Monster) {
                if (gunshotData != null && currentPos.distanceTo(gunshotData.position()) <= effectiveRange) {
                    target = findAccessiblePosition(gunshotData.position());
                }

                if (target != null) {
                    mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
                }
            } else {
                if (gunshotData != null && currentPos.distanceTo(gunshotData.position()) <= effectiveRange) {
                    Vec3 directionAway = currentPos.subtract(gunshotData.position()).normalize();
                    Vec3 fleeTarget = findAccessiblePosition(currentPos.add(directionAway.scale(effectiveRange)));
                    if (fleeTarget != null) {
                        mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, effectiveSpeed);
                    }
                }
            }
        }
    }

    private Vec3 findAccessiblePosition(Vec3 targetPos) {
        BlockPos targetBlock = new BlockPos(targetPos);

        if (isWalkable(targetBlock)) {
            return targetPos;
        }

        for (int radius = 1; radius <= 5; radius++) {
            for (int xOff = -radius; xOff <= radius; xOff++) {
                for (int zOff = -radius; zOff <= radius; zOff++) {
                    if (Math.abs(xOff) != radius && Math.abs(zOff) != radius) continue;

                    BlockPos checkPos = targetBlock.offset(xOff, 0, zOff);
                    if (isWalkable(checkPos)) {
                        return new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
                    }
                }
            }
        }

        for (int yOff = -3; yOff <= 3; yOff++) {
            BlockPos checkPos = targetBlock.offset(0, yOff, 0);
            if (isWalkable(checkPos)) {
                return new Vec3(checkPos.getX() + 0.5, checkPos.getY(), checkPos.getZ() + 0.5);
            }
        }

        return grounded(targetPos);
    }

    private boolean isWalkable(BlockPos pos) {
        if (!mob.level.isLoaded(pos)) {
            return false;
        }

        BlockPos below = pos.below();
        BlockState blockState = mob.level.getBlockState(below);
        if (!blockState.getMaterial().isSolid()) {
            return false;
        }

        return mob.level.getBlockState(pos).isAir() &&
                mob.level.getBlockState(pos.above()).isAir();
    }

    private Vec3 grounded(Vec3 desiredXZ) {
        BlockPos base = new BlockPos(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    private ResourceLocation toLocation(String soundStr) {
        if (soundStr.contains(":")) {
            return ResourceLocation.parse(soundStr);
        }
        return new ResourceLocation(soundStr);
    }
}