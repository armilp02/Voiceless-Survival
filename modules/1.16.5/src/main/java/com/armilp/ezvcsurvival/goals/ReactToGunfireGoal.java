package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.compat.TACFireListener;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.GunshotData;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.gen.Heightmap;

import java.util.EnumSet;

public class ReactToGunfireGoal extends Goal {
    private final MobEntity mob;
    private final double baseSpeed;
    private final double baseRange;

    private static final long PRIORITY_SOUND_DURATION_MS = 3000L;

    public ReactToGunfireGoal(MobEntity mob, double speed, double range) {
        this.mob = mob;
        this.baseSpeed = speed;
        this.baseRange = range;
        // Importante: establecer las flags del Goal
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) {
            return false;
        }

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null &&
                System.currentTimeMillis() - ReactToGeneralSoundGoal.lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
            ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
        }

        this.checkForPrioritySounds();

        Vector3d mobCenterPos = this.mob.position();

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null) {
            double priorityRange = this.baseRange * 1.5D;
            if (this.mob.level.isRaining() || this.mob.level.isThundering()) {
                priorityRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get().doubleValue();
            }
            if (mobCenterPos.distanceTo(ReactToGeneralSoundGoal.lastPrioritySoundPos) <= priorityRange) {
                return true;
            }
        }

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos == null) {
            double effectiveRange = this.baseRange;

            GunshotData gunshotData = TACFireListener.getLastGunshot();

            if (gunshotData != null) {
                String effectiveGunType = gunshotData.gunType;

                if (effectiveGunType != null && !effectiveGunType.isEmpty()) {
                    double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                    effectiveRange = this.baseRange * rangeMultiplier;
                }

                if (this.mob.level.isRaining() || this.mob.level.isThundering()) {
                    effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get().doubleValue();
                }

                return mobCenterPos.distanceTo(gunshotData.position) <= effectiveRange;
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob.getTarget() != null) {
            return false;
        }

        return !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.updateNavigation();
    }

    @Override
    public void tick() {
        this.updateNavigation();
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
    }

    private void checkForPrioritySounds() {
        for (SoundGroupData priority : SoundConfig.getPriorityGroups()) {
            for (String soundStr : priority.sounds) {
                ResourceLocation soundLoc = this.toLocation(soundStr);
                Vector3d priorityPos = SoundEventTracker.getLastPlayedPositionForSound(soundLoc);
                if (priorityPos != null) {
                    ReactToGeneralSoundGoal.setPrioritySound(priorityPos);
                    return;
                }
            }
        }

        GunshotData gunshotData = TACFireListener.getLastGunshot();
        if (gunshotData != null && this.isPriorityGunType(gunshotData.gunType)) {
            ReactToGeneralSoundGoal.setPrioritySound(gunshotData.position);
        }
    }

    private boolean isPriorityGunType(String gunType) {
        if (gunType == null || gunType.isEmpty()) {
            return false;
        }
        double rangeMultiplier = SoundConfig.getRangeMultiplier(gunType);
        return rangeMultiplier >= 6.0D;
    }

    private void updateNavigation() {
        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null &&
                System.currentTimeMillis() - ReactToGeneralSoundGoal.lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
            ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
        }

        this.checkForPrioritySounds();

        Vector3d currentPos = this.mob.position();
        Vector3d target = null;
        double effectiveRange = this.baseRange;
        double effectiveSpeed = this.baseSpeed;

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null) {
            effectiveRange = this.baseRange * 1.5D;
            effectiveSpeed = this.baseSpeed * 1.3D;

            if (this.mob.level.isRaining() || this.mob.level.isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get().doubleValue();
            }

            if (currentPos.distanceTo(ReactToGeneralSoundGoal.lastPrioritySoundPos) <= effectiveRange) {
                if (this.mob instanceof MonsterEntity) {
                    target = this.findAccessiblePosition(ReactToGeneralSoundGoal.lastPrioritySoundPos);
                } else {
                    Vector3d directionAway = currentPos.subtract(ReactToGeneralSoundGoal.lastPrioritySoundPos).normalize();
                    target = this.findAccessiblePosition(currentPos.add(directionAway.scale(effectiveRange)));
                }

                if (target != null) {
                    this.mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
                    if (currentPos.distanceTo(ReactToGeneralSoundGoal.lastPrioritySoundPos) < 2.0D) {
                        ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
                    }
                }
                return;
            }
        }

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos == null) {
            GunshotData gunshotData = TACFireListener.getLastGunshot();

            if (gunshotData != null) {
                String effectiveGunType = gunshotData.gunType;

                if (effectiveGunType != null && !effectiveGunType.isEmpty()) {
                    double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                    double speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
                    effectiveRange = this.baseRange * rangeMultiplier;
                    effectiveSpeed = this.baseSpeed * speedMultiplier;
                }

                if (this.mob.level.isRaining() || this.mob.level.isThundering()) {
                    effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get().doubleValue();
                }

                if (this.mob instanceof MonsterEntity) {
                    if (currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
                        target = this.findAccessiblePosition(gunshotData.position);
                    }

                    if (target != null) {
                        this.mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
                    }
                } else {
                    if (currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
                        Vector3d directionAway = currentPos.subtract(gunshotData.position).normalize();
                        Vector3d fleeTarget = this.findAccessiblePosition(currentPos.add(directionAway.scale(effectiveRange)));
                        if (fleeTarget != null) {
                            this.mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, effectiveSpeed);
                        }
                    }
                }
            }
        }
    }

    private Vector3d findAccessiblePosition(Vector3d targetPos) {
        BlockPos targetBlock = new BlockPos(targetPos);

        if (this.isWalkable(targetBlock)) {
            return targetPos;
        }

        for (int radius = 1; radius <= 5; radius++) {
            for (int xOff = -radius; xOff <= radius; xOff++) {
                for (int zOff = -radius; zOff <= radius; zOff++) {
                    if (Math.abs(xOff) != radius && Math.abs(zOff) != radius) {
                        continue;
                    }

                    BlockPos checkPos = targetBlock.offset(xOff, 0, zOff);
                    if (this.isWalkable(checkPos)) {
                        return new Vector3d(checkPos.getX() + 0.5D, checkPos.getY(), checkPos.getZ() + 0.5D);
                    }
                }
            }
        }

        for (int yOff = -3; yOff <= 3; yOff++) {
            BlockPos checkPos = targetBlock.offset(0, yOff, 0);
            if (this.isWalkable(checkPos)) {
                return new Vector3d(checkPos.getX() + 0.5D, checkPos.getY(), checkPos.getZ() + 0.5D);
            }
        }

        return this.grounded(targetPos);
    }

    private boolean isWalkable(BlockPos pos) {
        if (!this.mob.level.isLoaded(pos)) {
            return false;
        }

        BlockPos below = pos.below();
        BlockState blockState = this.mob.level.getBlockState(below);
        if (!blockState.getMaterial().isSolid()) {
            return false;
        }

        return this.mob.level.getBlockState(pos).isAir() &&
                this.mob.level.getBlockState(pos.above()).isAir();
    }

    private Vector3d grounded(Vector3d desiredXZ) {
        BlockPos base = new BlockPos(desiredXZ.x, 0.0D, desiredXZ.z);
        BlockPos top = this.mob.level.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vector3d(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D);
    }

    private ResourceLocation toLocation(String soundStr) {
        if (soundStr.contains(":")) {
            String[] parts = soundStr.split(":");
            return new ResourceLocation(parts[0], parts[1]);
        }
        return new ResourceLocation(soundStr);
    }
}