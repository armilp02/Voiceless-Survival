package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.compat.tacz.GunFireListener;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.GunTypeModifiers;
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

import java.util.ArrayList;
import java.util.List;

public class ReactToGunfireGoal extends Goal {

    private static final long PRIORITY_SOUND_DURATION_MS = 3000;
    private static final double STUCK_THRESHOLD_SQ = 0.04;
    private static final int STUCK_CHECK_INTERVAL = 20;
    private static final int STUCK_MAX_TICKS = 60;

    private final Mob mob;
    private final double baseSpeed;
    private final double baseRange;
    private final boolean isMonster;
    private final List<ResourceLocation> prioritySoundLocations;

    private Vec3 cachedTarget = null;
    private double cachedSpeed = 0;
    private int tickCounter = 0;
    private Vec3 lastCheckedPos = null;
    private int stuckTicks = 0;

    private long lastReactedShotTimestamp = -1;
    private long lastReactedPriorityTimestamp = -1;

    public ReactToGunfireGoal(Mob mob, double speed, double range) {
        this.mob = mob;
        this.baseSpeed = speed;
        this.baseRange = range;
        this.isMonster = mob instanceof Monster;
        this.prioritySoundLocations = buildPrioritySoundLocations();
    }

    private static List<ResourceLocation> buildPrioritySoundLocations() {
        List<SoundGroupData> groups = SoundConfig.getPriorityGroups();
        List<ResourceLocation> result = new ArrayList<>();
        for (SoundGroupData group : groups) {
            for (String s : group.sounds()) {
                result.add(ResourceLocation.parse(s));
            }
        }
        return result;
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) return false;

        long now = System.currentTimeMillis();
        expirePrioritySound(now);

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null) {
            if (ReactToGeneralSoundGoal.lastPrioritySoundTimestamp == lastReactedPriorityTimestamp) return false;
            double range = priorityRange();
            Vec3 mobPos = mob.position();
            return mobPos.distanceToSqr(ReactToGeneralSoundGoal.lastPrioritySoundPos) <= range * range;
        }

        GunshotData data = GunFireListener.getLastGunshotData();
        if (data == null) return false;
        if (data.timestamp() == lastReactedShotTimestamp) return false;

        tryRegisterPrioritySounds(data);

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null) {
            if (ReactToGeneralSoundGoal.lastPrioritySoundTimestamp == lastReactedPriorityTimestamp) return false;
            double range = priorityRange();
            Vec3 mobPos = mob.position();
            return mobPos.distanceToSqr(ReactToGeneralSoundGoal.lastPrioritySoundPos) <= range * range;
        }

        double range = gunRange(data);
        Vec3 mobPos = mob.position();
        return mobPos.distanceToSqr(data.position()) <= range * range;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) return false;
        if (cachedTarget == null) return false;
        if (stuckTicks >= STUCK_MAX_TICKS) return false;
        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        cachedTarget = null;
        cachedSpeed = baseSpeed;
        tickCounter = 0;
        stuckTicks = 0;
        lastCheckedPos = mob.position();

        long now = System.currentTimeMillis();
        expirePrioritySound(now);

        Vec3 mobPos = mob.position();

        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null) {
            double range = priorityRange();
            double speed = baseSpeed * 1.3;
            if (mobPos.distanceToSqr(ReactToGeneralSoundGoal.lastPrioritySoundPos) <= range * range) {
                Vec3 dest = resolveDestination(mobPos, ReactToGeneralSoundGoal.lastPrioritySoundPos, range);
                if (dest != null) {
                    cachedTarget = dest;
                    cachedSpeed = speed;
                    lastReactedPriorityTimestamp = ReactToGeneralSoundGoal.lastPrioritySoundTimestamp;
                    mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
                }
            }
            return;
        }

        GunshotData data = GunFireListener.getLastGunshotData();
        if (data == null) return;

        String gunType = data.gunType().name().toLowerCase();
        double rangeMult = SoundConfig.getRangeMultiplier(gunType);
        double speedMult = SoundConfig.getSpeedMultiplier(gunType);

        if (GunFireListener.wasLastShotSilenced()) {
            GunTypeModifiers silencerMod = SoundConfig.getSilencerModifiers(gunType);
            rangeMult *= silencerMod.rangeMultiplier();
            speedMult *= silencerMod.speedMultiplier();
        }

        double range = baseRange * rangeMult;
        double speed = baseSpeed * speedMult;
        if (isWeatherActive()) range *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();

        if (mobPos.distanceToSqr(data.position()) <= range * range) {
            Vec3 dest = resolveDestination(mobPos, data.position(), range);
            if (dest != null) {
                cachedTarget = dest;
                cachedSpeed = speed;
                lastReactedShotTimestamp = data.timestamp();
                mob.getNavigation().moveTo(dest.x, dest.y, dest.z, speed);
            }
        }
    }

    @Override
    public void tick() {
        if (cachedTarget == null) return;
        tickCounter++;

        if (tickCounter % STUCK_CHECK_INTERVAL == 0) {
            Vec3 mobPos = mob.position();
            if (lastCheckedPos != null && mobPos.distanceToSqr(lastCheckedPos) < STUCK_THRESHOLD_SQ) {
                stuckTicks += STUCK_CHECK_INTERVAL;
                if (stuckTicks < STUCK_MAX_TICKS) {
                    mob.getNavigation().moveTo(cachedTarget.x, cachedTarget.y, cachedTarget.z, cachedSpeed);
                }
            } else {
                stuckTicks = 0;
            }
            lastCheckedPos = mobPos;
        }
    }

    @Override
    public void stop() {
        cachedTarget = null;
        tickCounter = 0;
        stuckTicks = 0;
        lastCheckedPos = null;
        mob.getNavigation().stop();
    }

    private void tryRegisterPrioritySounds(GunshotData data) {
        for (ResourceLocation loc : prioritySoundLocations) {
            Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(loc);
            if (pos != null) {
                ReactToGeneralSoundGoal.setPrioritySound(pos);
                return;
            }
        }
        if (data != null) {
            String gunType = data.gunType().name().toLowerCase();
            if (SoundConfig.getRangeMultiplier(gunType) >= 6.0) {
                ReactToGeneralSoundGoal.setPrioritySound(data.position());
            }
        }
    }

    private double priorityRange() {
        double range = baseRange * 1.5;
        if (isWeatherActive()) range *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        return range;
    }

    private double gunRange(GunshotData data) {
        String gunType = data.gunType().name().toLowerCase();
        double rangeMult = SoundConfig.getRangeMultiplier(gunType);

        if (GunFireListener.wasLastShotSilenced()) {
            rangeMult *= SoundConfig.getSilencerModifiers(gunType).rangeMultiplier();
        }

        double range = baseRange * rangeMult;
        if (isWeatherActive()) range *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        return range;
    }

    private void expirePrioritySound(long now) {
        if (ReactToGeneralSoundGoal.lastPrioritySoundPos != null &&
                now - ReactToGeneralSoundGoal.lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
            ReactToGeneralSoundGoal.lastPrioritySoundPos = null;
        }
    }

    private Vec3 resolveDestination(Vec3 mobPos, Vec3 soundPos, double range) {
        if (isMonster) {
            return findAccessiblePosition(soundPos);
        } else {
            Vec3 fleeDir = mobPos.subtract(soundPos);
            double length = fleeDir.length();
            if (length < 0.001) return null;
            return findAccessiblePosition(mobPos.add(fleeDir.normalize().scale(range)));
        }
    }

    private boolean isWeatherActive() {
        return mob.level.isRaining() || mob.level.isThundering();
    }

    private Vec3 findAccessiblePosition(Vec3 targetPos) {
        BlockPos targetBlock = new BlockPos(targetPos);

        if (isWalkable(targetBlock)) return targetPos;

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
}