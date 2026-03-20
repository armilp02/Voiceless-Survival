package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
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

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

public class ReactToGeneralSoundGoal extends Goal {

    private static final long PRIORITY_SOUND_DURATION_MS = 3500;
    private static final long SOUND_EXPIRY_MS = 5000;
    private static final long MIN_REACTION_INTERVAL_MS = 500;
    private static final int LOOK_UPDATE_INTERVAL = 30;
    private static final double STUCK_THRESHOLD_SQ = 0.04;
    private static final int STUCK_CHECK_INTERVAL = 20;
    private static final int STUCK_MAX_TICKS = 60;
    private static final double ARRIVAL_DISTANCE_SQ = 4.0;

    // Shared priority sound state — written by both goal types, read in canUse()
    public static volatile Vec3 lastPrioritySoundPos = null;
    public static volatile long lastPrioritySoundTimestamp = 0;

    private final Mob mob;
    private final double baseSpeed;
    private final double baseRange;
    private final String entityId;
    private final boolean isMonster;
    // Stagger stuck-checks across mobs to avoid same-tick spikes
    private final int tickOffset;

    private Vec3 targetSoundPos = null;
    private double activeSpeed = 1.0;
    private long targetSetTime = 0;
    private long lastReactedSoundTimestamp = -1;
    private int tickCounter = 0;
    private Vec3 lastCheckedPos = null;
    private int stuckTicks = 0;
    private long lastStartTimeMs = 0;

    public ReactToGeneralSoundGoal(Mob mob, double speed, double range) {
        this.mob = mob;
        this.baseSpeed = speed;
        this.baseRange = range;
        this.entityId = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType())).toString();
        this.isMonster = mob instanceof Monster;
        this.tickOffset = Math.abs(mob.getId() % STUCK_CHECK_INTERVAL);
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) return false;

        long now = System.currentTimeMillis();
        if (now - lastStartTimeMs < MIN_REACTION_INTERVAL_MS) return false;
        if (!GeneralSoundsConfig.isEnabled()) return false;

        GeneralSoundsConfig.Reaction mobReaction = GeneralSoundsConfig.getMobReactions().get(entityId);
        if (mobReaction == null || !mobReaction.enabled) return false;

        // Priority sound takes precedence over individual sound entries
        Vec3 capturedPriority = lastPrioritySoundPos;
        if (capturedPriority != null) {
            if (now - lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
                lastPrioritySoundPos = null;
            } else {
                double effectiveRange = mobReaction.range * 1.5 * weatherMultiplier();
                if (mob.position().distanceToSqr(capturedPriority) <= effectiveRange * effectiveRange) {
                    targetSoundPos = capturedPriority;
                    activeSpeed = mobReaction.speed * 1.5;
                    targetSetTime = now;
                    return true;
                }
            }
        }

        Collection<Map.Entry<ResourceLocation, TimedSoundData>> entries = SoundEventTracker.getActiveEntries();
        if (entries.isEmpty()) return false;

        Map<String, GeneralSoundsConfig.SoundEntry> soundMap = GeneralSoundsConfig.getSounds();
        if (soundMap == null || soundMap.isEmpty()) return false;

        Vec3 bestPos = null;
        double bestSpeedMult = 1.0;
        double bestDistSq = Double.MAX_VALUE;
        long bestTimestamp = -1;

        for (Map.Entry<ResourceLocation, TimedSoundData> entry : entries) {
            TimedSoundData soundData = entry.getValue();
            if (soundData.timestamp() == lastReactedSoundTimestamp) continue;

            String soundId = entry.getKey().toString();
            GeneralSoundsConfig.SoundEntry soundEntry = soundMap.get(soundId);
            if (soundEntry == null || !soundEntry.enabled) continue;
            if (!GeneralSoundsConfig.canEntityReactToSound(entityId, soundId)) continue;

            double effectiveRange = mobReaction.range * soundEntry.range_multiplier * weatherMultiplier();
            double distSq = mob.position().distanceToSqr(soundData.position());
            if (distSq > effectiveRange * effectiveRange) continue;

            if (soundEntry.is_priority) setPrioritySound(soundData.position());

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestPos = soundData.position();
                bestSpeedMult = soundEntry.speed_multiplier;
                bestTimestamp = soundData.timestamp();
            }
        }

        if (bestPos == null) return false;

        targetSoundPos = bestPos;
        activeSpeed = mobReaction.speed * bestSpeedMult;
        targetSetTime = now;
        lastReactedSoundTimestamp = bestTimestamp;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) return false;
        if (targetSoundPos == null) return false;
        if (stuckTicks >= STUCK_MAX_TICKS) return false;
        if (mob.position().distanceToSqr(targetSoundPos) <= ARRIVAL_DISTANCE_SQ) return false;
        return System.currentTimeMillis() - targetSetTime <= SOUND_EXPIRY_MS;
    }

    @Override
    public void start() {
        lastStartTimeMs = System.currentTimeMillis();
        tickCounter = 0;
        stuckTicks = 0;
        lastCheckedPos = mob.position();
        issueMoveTo();
    }

    @Override
    public void tick() {
        if (targetSoundPos == null) return;
        tickCounter++;
        if ((tickCounter + tickOffset) % STUCK_CHECK_INTERVAL != 0) return;

        Vec3 mobPos = mob.position();

        if (mob.getNavigation().isDone()) {
            if (mobPos.distanceToSqr(targetSoundPos) > ARRIVAL_DISTANCE_SQ) {
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

        if (isMonster && tickCounter % LOOK_UPDATE_INTERVAL == 0) {
            mob.getLookControl().setLookAt(targetSoundPos.x, targetSoundPos.y, targetSoundPos.z, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        targetSoundPos = null;
        tickCounter = 0;
        stuckTicks = 0;
        lastCheckedPos = null;
    }

    private void issueMoveTo() {
        if (targetSoundPos == null) return;
        Vec3 dest = isMonster
                ? grounded(targetSoundPos)
                : grounded(mob.position().add(
                mob.position().subtract(targetSoundPos).normalize().scale(baseRange)));
        mob.getNavigation().moveTo(dest.x, dest.y, dest.z, activeSpeed);
    }


    // Rain/thunder reduces effective hearing range.
    private double weatherMultiplier() {
        return (mob.level.isRaining() || mob.level.isThundering())
                ? SoundConfig.THUNDER_RANGE_MULTIPLIER.get()
                : 1.0;
    }

    private Vec3 grounded(Vec3 pos) {
        BlockPos top = mob.level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(pos.x, 0, pos.z));
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    public static void setPrioritySound(Vec3 position) {
        lastPrioritySoundPos = position;
        lastPrioritySoundTimestamp = System.currentTimeMillis();
    }
}