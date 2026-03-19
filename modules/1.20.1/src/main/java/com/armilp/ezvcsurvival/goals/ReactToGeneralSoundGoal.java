package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.data.TimedSoundData;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class ReactToGeneralSoundGoal extends Goal {

    private static final long PRIORITY_SOUND_DURATION_MS = 3500;
    private static final long SOUND_REACTION_TIMEOUT = 8000;
    private static final long MIN_REACTION_INTERVAL_MS = 5000;
    private static final long MAX_REACTION_JITTER_MS = 3000;
    private static final int LOOK_UPDATE_INTERVAL = 30;
    private static final double STUCK_THRESHOLD_SQ = 0.04;
    private static final int STUCK_CHECK_INTERVAL = 20;
    private static final int STUCK_MAX_TICKS = 60;
    private static final int CHECK_COOLDOWN_TICKS = 10;

    private static final double MAX_PATH_STEP = 32.0;
    private static final double ARRIVAL_DISTANCE_SQ = 9.0;

    public static volatile Vec3 lastPrioritySoundPos = null;
    public static volatile long lastPrioritySoundTimestamp = 0;

    private final Mob mob;
    private final double fallbackSpeed;
    private final double fallbackRange;
    private final String entityId;
    private final boolean isMonster;

    private final Map<ResourceLocation, ResolvedSoundEntry> filteredSoundsMap;

    private Vec3 actualTarget = null;
    private Vec3 cachedStepTarget = null;
    private double cachedSpeed = 0;
    private long targetSetTime = 0;
    private int tickCounter = 0;
    private Vec3 lastCheckedPos = null;
    private int stuckTicks = 0;
    private int checkCooldown = 0;

    private long lastReactedPriorityTimestamp = -1;
    private long lastReactedSoundTimestamp = -1;
    private long lastKnownSoundVersion = -1;
    private long lastReactionTimeMs = 0;

    private final long reactionJitter;

    private Vec3 pendingSoundPos = null;
    private double pendingSpeed = 0;
    private double pendingEffectiveRange = 0;
    private boolean pendingIsPriority = false;

    private record ResolvedSoundEntry(SoundGroupData groupData, boolean isPriority) {
    }

    public ReactToGeneralSoundGoal(Mob mob, double fallbackSpeed, double fallbackRange,
                                   List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.fallbackSpeed = fallbackSpeed;
        this.fallbackRange = fallbackRange;
        this.entityId = Objects.requireNonNull(
                ForgeRegistries.ENTITY_TYPES.getKey(mob.getType())).toString();
        this.isMonster = mob instanceof Monster;
        this.filteredSoundsMap = buildFilteredSoundsMap(soundGroups);
        this.reactionJitter = (long) (new Random().nextDouble() * MAX_REACTION_JITTER_MS);
    }

    private Map<ResourceLocation, ResolvedSoundEntry> buildFilteredSoundsMap(
            List<SoundGroupData> soundGroups) {
        Map<String, GeneralSoundsConfig.SoundEntry> soundMap = GeneralSoundsConfig.getSounds();
        Map<ResourceLocation, ResolvedSoundEntry> result = new HashMap<>();
        for (SoundGroupData group : soundGroups) {
            boolean isPriority = group.groupName().startsWith("auto_priority_");
            for (String soundStr : group.sounds()) {
                GeneralSoundsConfig.SoundEntry entry = soundMap.get(soundStr);
                if (entry == null || !entry.enabled) continue;
                if (!GeneralSoundsConfig.canEntityReactToSound(entityId, soundStr)) continue;
                result.put(new ResourceLocation(soundStr),
                        new ResolvedSoundEntry(group, isPriority));
            }
        }
        return result;
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) return false;

        long now = System.currentTimeMillis();
        if ((now - lastReactionTimeMs) < (MIN_REACTION_INTERVAL_MS + reactionJitter)) return false;

        boolean hasNewPriority = lastPrioritySoundPos != null &&
                lastPrioritySoundTimestamp != lastReactedPriorityTimestamp;

        if (checkCooldown > 0) {
            checkCooldown--;
            if (!hasNewPriority) return false;
            GeneralSoundsConfig.Reaction reaction =
                    GeneralSoundsConfig.getMobReactions().get(entityId);
            double mobRange = reaction != null ? reaction.range : fallbackRange;
            double effectiveRangeSq = (mobRange * 1.5) * (mobRange * 1.5);
            if (mob.position().distanceToSqr(lastPrioritySoundPos) > effectiveRangeSq)
                return false;
        }

        long currentVersion = SoundEventTracker.globalSoundVersion;
        if (!hasNewPriority && currentVersion == lastKnownSoundVersion) {
            checkCooldown = CHECK_COOLDOWN_TICKS;
            return false;
        }
        lastKnownSoundVersion = currentVersion;

        GeneralSoundsConfig.Reaction reaction =
                GeneralSoundsConfig.getMobReactions().get(entityId);
        if (reaction != null && !reaction.enabled) return false;

        double mobSpeed = reaction != null ? reaction.speed : fallbackSpeed;
        double mobRange = reaction != null ? reaction.range : fallbackRange;

        expirePrioritySound(now);

        if (hasNewPriority) {
            Vec3 mobPos = mob.position();
            double effectiveRange = mobRange * 1.5;
            if (isWeatherActive()) effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            if (mobPos.distanceToSqr(lastPrioritySoundPos) <= effectiveRange * effectiveRange) {
                pendingSoundPos = lastPrioritySoundPos;
                pendingSpeed = mobSpeed * 1.5;
                pendingEffectiveRange = effectiveRange;
                pendingIsPriority = true;
                targetSetTime = now;
                return true;
            }
        }

        if (SoundEventTracker.getActiveCount() == 0) return false;

        boolean found = findSoundTarget(now, mobSpeed, mobRange);
        if (!found) checkCooldown = CHECK_COOLDOWN_TICKS;
        return found;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) return false;
        if (actualTarget == null) return false;
        if (stuckTicks >= STUCK_MAX_TICKS) return false;

        if (mob.position().distanceToSqr(actualTarget) <= ARRIVAL_DISTANCE_SQ) return false;

        long now = System.currentTimeMillis();
        expirePrioritySound(now);

        if (lastPrioritySoundPos != null &&
                (now - lastPrioritySoundTimestamp) <= PRIORITY_SOUND_DURATION_MS) {
            return true;
        }

        return (now - targetSetTime) <= SOUND_REACTION_TIMEOUT;
    }

    @Override
    public void start() {
        lastReactionTimeMs = System.currentTimeMillis();
        tickCounter = 0;
        stuckTicks = 0;
        checkCooldown = 0;
        lastCheckedPos = mob.position();

        if (pendingIsPriority) {
            lastReactedPriorityTimestamp = lastPrioritySoundTimestamp;
        }

        if (pendingSoundPos != null) {
            actualTarget = resolveActualDestination(mob.position(), pendingSoundPos,
                    pendingEffectiveRange);
            if (actualTarget != null) {
                cachedSpeed = pendingSpeed;
                cachedStepTarget = computeStep(mob.position(), actualTarget);
                issueMoveTo(cachedStepTarget);
            }
            pendingSoundPos = null;
            pendingEffectiveRange = 0;
            pendingIsPriority = false;
        }
    }

    @Override
    public void tick() {
        if (actualTarget == null) return;
        tickCounter++;

        if (tickCounter % STUCK_CHECK_INTERVAL == 0) {
            Vec3 mobPos = mob.position();

            if (mob.getNavigation().isDone()) {
                double distSq = mobPos.distanceToSqr(actualTarget);
                if (distSq > ARRIVAL_DISTANCE_SQ) {
                    cachedStepTarget = computeStep(mobPos, actualTarget);
                    issueMoveTo(cachedStepTarget);
                    stuckTicks = 0;
                }
            } else {
                if (lastCheckedPos != null &&
                        mobPos.distanceToSqr(lastCheckedPos) < STUCK_THRESHOLD_SQ) {
                    stuckTicks += STUCK_CHECK_INTERVAL;
                    if (stuckTicks < STUCK_MAX_TICKS) {
                        issueMoveTo(cachedStepTarget);
                    }
                } else {
                    stuckTicks = 0;
                }
            }

            lastCheckedPos = mobPos;
        }

        if (isMonster && tickCounter % LOOK_UPDATE_INTERVAL == 0 && actualTarget != null) {
            mob.getLookControl().setLookAt(
                    actualTarget.x, actualTarget.y, actualTarget.z, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        lastReactionTimeMs = System.currentTimeMillis();
        actualTarget = null;
        cachedStepTarget = null;
        cachedSpeed = 0;
        tickCounter = 0;
        stuckTicks = 0;
        checkCooldown = CHECK_COOLDOWN_TICKS;
        lastCheckedPos = null;
        pendingSoundPos = null;
        pendingSpeed = 0;
        pendingEffectiveRange = 0;
        pendingIsPriority = false;
        mob.getNavigation().stop();
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    /*
     * Iterates the TRACKER (typically 1-5 active entries) and looks up in
     * filteredSoundsMap (O(1)) — much cheaper than iterating 200+ filteredSounds
     * entries and doing a map lookup on each.
     */
    private boolean findSoundTarget(long now, double mobBaseSpeed, double mobBaseRange) {
        Vec3 mobPos = mob.position();

        for (Map.Entry<ResourceLocation, TimedSoundData> tracked :
                SoundEventTracker.getActiveEntries()) {
            ResolvedSoundEntry entry = filteredSoundsMap.get(tracked.getKey());
            if (entry == null) continue;

            TimedSoundData data = tracked.getValue();
            if (data.timestamp() == lastReactedSoundTimestamp) continue;

            double effectiveRange = mobBaseRange * entry.groupData.rangeMultiplier();
            if (isWeatherActive()) effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();

            if (mobPos.distanceToSqr(data.position()) > effectiveRange * effectiveRange) continue;

            pendingSoundPos = data.position();
            pendingSpeed = mobBaseSpeed * entry.groupData.speedMultiplier()
                    * (entry.isPriority ? 1.3 : 1.0);
            pendingEffectiveRange = effectiveRange;
            pendingIsPriority = entry.isPriority;
            targetSetTime = now;
            lastReactedSoundTimestamp = data.timestamp();

            if (entry.isPriority && lastPrioritySoundPos == null) {
                setPrioritySound(data.position());
            }

            return true;
        }

        return false;
    }

    /*
     * Computes the mob's ultimate destination (actual sound pos for monsters,
     * flee point for passive mobs). Does NOT cap distance — that is done by
     * computeStep().
     */
    private Vec3 resolveActualDestination(Vec3 mobPos, Vec3 soundPos, double effectiveRange) {
        if (isMonster) {
            return grounded(soundPos);
        } else {
            Vec3 fleeDir = mobPos.subtract(soundPos);
            double length = fleeDir.length();
            if (length < 0.001) return null;
            return grounded(mobPos.add(fleeDir.normalize().scale(effectiveRange)));
        }
    }

    private Vec3 computeStep(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double dist = dir.length();
        if (dist <= MAX_PATH_STEP) return to;
        return grounded(from.add(dir.normalize().scale(MAX_PATH_STEP)));
    }

    private void issueMoveTo(Vec3 target) {
        if (target == null) return;
        mob.getNavigation().moveTo(target.x, target.y, target.z, cachedSpeed);
    }

    private void expirePrioritySound(long now) {
        if (lastPrioritySoundPos != null &&
                (now - lastPrioritySoundTimestamp) > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }
    }

    private boolean isWeatherActive() {
        return mob.level().isRaining() || mob.level().isThundering();
    }

    private Vec3 grounded(Vec3 desiredXZ) {
        BlockPos base = BlockPos.containing(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.level().getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    public static void setPrioritySound(Vec3 position) {
        lastPrioritySoundPos = position;
        lastPrioritySoundTimestamp = System.currentTimeMillis();
    }
}