package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.compat.tacz.GunFireListener;
import com.armilp.ezvcsurvival.config.GunfireConfig;
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
import java.util.EnumSet;
import java.util.List;

public class ReactToGunfireGoal extends Goal {

    private static final long   MIN_REACTION_INTERVAL_MS = 500;
    private static final long   SHOT_EXPIRY_MS           = 4000;
    private static final double STUCK_THRESHOLD_SQ       = 0.04;
    private static final int    STUCK_CHECK_INTERVAL     = 20;
    private static final int    STUCK_MAX_TICKS          = 60;
    private static final double ARRIVAL_DISTANCE_SQ      = 4.0;
    private static final double SAME_POS_THRESHOLD_SQ    = 4.0;

    private final Mob mob;
    private final double baseSpeed;
    private final double baseRange;
    private final boolean isMonster;
    private final List<ResourceLocation> prioritySoundLocations;
    // Stagger stuck-checks across mobs to avoid same-tick spikes
    private final int tickOffset;

    private Vec3   cachedTarget             = null;
    private Vec3   lastSoundPos             = null;
    private double cachedSpeed              = 0;
    private int    tickCounter              = 0;
    private Vec3   lastCheckedPos           = null;
    private int    stuckTicks               = 0;
    private long   lastReactionTimeMs       = 0;
    private long   lastReactedShotTimestamp = -1;

    public ReactToGunfireGoal(Mob mob, double speed, double range) {
        this.mob      = mob;
        this.baseSpeed = speed;
        this.baseRange = range;
        this.isMonster = mob instanceof Monster;
        this.prioritySoundLocations = buildPrioritySoundLocations();
        this.tickOffset = Math.abs(mob.getId() % STUCK_CHECK_INTERVAL);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private static List<ResourceLocation> buildPrioritySoundLocations() {
        List<SoundGroupData> groups = SoundConfig.getPriorityGroups();
        List<ResourceLocation> result = new ArrayList<>(groups.size() * 2);
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
        if (!GunfireConfig.isEnabled()) return false;

        long now = System.currentTimeMillis();
        if (now - lastReactionTimeMs < MIN_REACTION_INTERVAL_MS) return false;

        GunshotData data = GunFireListener.getLastGunshotData();
        if (data == null) return false;
        if (data.timestamp() == lastReactedShotTimestamp) return false;
        if (now - data.timestamp() > SHOT_EXPIRY_MS) return false;

        tryRegisterPrioritySounds(data);

        // Only use priorityPos if it was registered at or after this shot — not a stale one
        Vec3 priorityPos = freshPriorityPos(data.timestamp());
        if (priorityPos != null) {
            double range = boostedRange();
            return mob.position().distanceToSqr(priorityPos) <= range * range;
        }

        double range = gunRange(data);
        return mob.position().distanceToSqr(data.position()) <= range * range;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) return false;
        if (cachedTarget == null) return false;
        if (stuckTicks >= STUCK_MAX_TICKS) return false;
        return mob.position().distanceToSqr(cachedTarget) > ARRIVAL_DISTANCE_SQ;
    }

    @Override
    public void start() {
        cachedTarget   = null;
        lastSoundPos   = null;
        cachedSpeed    = baseSpeed;
        tickCounter    = 0;
        stuckTicks     = 0;
        lastCheckedPos = mob.position();
        lastReactionTimeMs = System.currentTimeMillis();

        GunshotData data = GunFireListener.getLastGunshotData();
        if (data != null) applyShot(data);
    }

    @Override
    public void tick() {
        if (cachedTarget == null) return;
        tickCounter++;
        if ((tickCounter + tickOffset) % STUCK_CHECK_INTERVAL != 0) return;

        Vec3 mobPos = mob.position();

        // React immediately to a newer shot while still moving
        GunshotData latest = GunFireListener.getLastGunshotData();
        if (latest != null
                && latest.timestamp() != lastReactedShotTimestamp
                && System.currentTimeMillis() - latest.timestamp() <= SHOT_EXPIRY_MS) {
            tryRegisterPrioritySounds(latest);
            applyShot(latest);
            stuckTicks     = 0;
            lastCheckedPos = mobPos;
            return;
        }

        if (mob.getNavigation().isDone()) {
            if (mobPos.distanceToSqr(cachedTarget) > ARRIVAL_DISTANCE_SQ) {
                issueMoveTo();
                stuckTicks = 0;
            }
        } else {
            if (lastCheckedPos != null && mobPos.distanceToSqr(lastCheckedPos) < STUCK_THRESHOLD_SQ) {
                stuckTicks += STUCK_CHECK_INTERVAL;
                if (stuckTicks < STUCK_MAX_TICKS) issueMoveTo();
            } else {
                stuckTicks = 0;
            }
        }

        lastCheckedPos = mobPos;
    }

    @Override
    public void stop() {
        // Preserve lastReactedShotTimestamp so canUse() doesn't re-trigger on the same shot
        lastReactionTimeMs = System.currentTimeMillis();
        cachedTarget   = null;
        lastSoundPos   = null;
        tickCounter    = 0;
        stuckTicks     = 0;
        lastCheckedPos = null;
    }

    private void applyShot(GunshotData data) {
        lastReactedShotTimestamp = data.timestamp();

        // Only use priorityPos if it was registered at or after this shot — not a stale one
        Vec3 priorityPos = freshPriorityPos(data.timestamp());
        Vec3 soundPos    = priorityPos != null ? priorityPos : data.position();

        double speed, range;
        if (priorityPos != null) {
            range = boostedRange();
            speed = baseSpeed * 1.3;
        } else {
            String gunType   = data.gunType().name().toLowerCase();
            double rangeMult = SoundConfig.getRangeMultiplier(gunType);
            double speedMult = SoundConfig.getSpeedMultiplier(gunType);
            if (data.silenced()) {
                GunTypeModifiers silencerMod = SoundConfig.getSilencerModifiers(gunType);
                rangeMult *= silencerMod.rangeMultiplier();
                speedMult *= silencerMod.speedMultiplier();
            }
            range = baseRange * rangeMult * weatherMultiplier();
            speed = baseSpeed * speedMult;
        }

        Vec3 mobPos = mob.position();
        if (mobPos.distanceToSqr(soundPos) > range * range) return;

        // Same position as before — just refresh speed and reissue
        if (lastSoundPos != null
                && soundPos.distanceToSqr(lastSoundPos) <= SAME_POS_THRESHOLD_SQ
                && cachedTarget != null) {
            cachedSpeed = speed;
            issueMoveTo();
            return;
        }

        Vec3 dest = resolveDestination(mobPos, soundPos, range);
        if (dest != null) {
            cachedTarget = dest;
            cachedSpeed  = speed;
            lastSoundPos = soundPos;
            issueMoveTo();
        }
    }

    private void issueMoveTo() {
        if (cachedTarget == null) return;
        mob.getNavigation().moveTo(cachedTarget.x, cachedTarget.y, cachedTarget.z, cachedSpeed);
    }

    private void tryRegisterPrioritySounds(GunshotData data) {
        for (ResourceLocation loc : prioritySoundLocations) {
            Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(loc);
            if (pos != null) {
                ReactToGeneralSoundGoal.setPrioritySound(pos);
                return;
            }
        }
        // Treat very loud guns as priority sounds directly
        String gunType = data.gunType().name().toLowerCase();
        if (SoundConfig.getRangeMultiplier(gunType) >= 6.0) {
            ReactToGeneralSoundGoal.setPrioritySound(data.position());
        }
    }

    // Returns lastPrioritySoundPos only if it was set at or after the given shot timestamp
    private static Vec3 freshPriorityPos(long shotTimestamp) {
        Vec3 pos = ReactToGeneralSoundGoal.lastPrioritySoundPos;
        if (pos == null) return null;
        return ReactToGeneralSoundGoal.lastPrioritySoundTimestamp >= shotTimestamp ? pos : null;
    }

    private double gunRange(GunshotData data) {
        String gunType   = data.gunType().name().toLowerCase();
        double rangeMult = SoundConfig.getRangeMultiplier(gunType);
        if (data.silenced()) rangeMult *= SoundConfig.getSilencerModifiers(gunType).rangeMultiplier();
        return baseRange * rangeMult * weatherMultiplier();
    }

    private double boostedRange() {
        return baseRange * 1.5 * weatherMultiplier();
    }

    // Rain/thunder reduces effective hearing range.
    private double weatherMultiplier() {
        return (mob.level.isRaining() || mob.level.isThundering())
                ? SoundConfig.THUNDER_RANGE_MULTIPLIER.get()
                : 1.0;
    }

    private Vec3 resolveDestination(Vec3 mobPos, Vec3 soundPos, double range) {
        if (isMonster) {
            return findAccessiblePosition(soundPos);
        }
        Vec3 fleeDir = mobPos.subtract(soundPos);
        return fleeDir.length() < 0.001 ? null
                : findAccessiblePosition(mobPos.add(fleeDir.normalize().scale(range)));
    }

    private Vec3 findAccessiblePosition(Vec3 targetPos) {
        BlockPos targetBlock = new BlockPos(targetPos);
        if (isWalkable(targetBlock)) return targetPos;

        for (int radius = 1; radius <= 5; radius++) {
            for (int xOff = -radius; xOff <= radius; xOff++) {
                for (int zOff = -radius; zOff <= radius; zOff++) {
                    if (Math.abs(xOff) != radius && Math.abs(zOff) != radius) continue;
                    BlockPos candidate = targetBlock.offset(xOff, 0, zOff);
                    if (isWalkable(candidate))
                        return new Vec3(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
                }
            }
        }

        for (int yOff = -3; yOff <= 3; yOff++) {
            BlockPos candidate = targetBlock.offset(0, yOff, 0);
            if (isWalkable(candidate))
                return new Vec3(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
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

    private Vec3 grounded(Vec3 pos) {
        BlockPos top = mob.level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(pos.x, 0, pos.z));
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }
}