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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReactToGeneralSoundGoal extends Goal {

    private static final long PRIORITY_SOUND_DURATION_MS = 3500;
    private static final long SOUND_REACTION_TIMEOUT = 4000;
    private static final int LOOK_UPDATE_INTERVAL = 30;
    private static final double STUCK_THRESHOLD_SQ = 0.04;
    private static final int STUCK_CHECK_INTERVAL = 20;
    private static final int STUCK_MAX_TICKS = 60;

    public static Vec3 lastPrioritySoundPos = null;
    public static long lastPrioritySoundTimestamp = 0;

    private final Mob mob;
    private final double speed;
    private final double range;
    private final List<SoundGroupData> soundGroups;
    private final String entityId;
    private final boolean isMonster;
    private final List<ResolvedSoundGroup> resolvedGroups;

    private Vec3 cachedTarget = null;
    private double cachedSpeed = 0;
    private long targetSetTime = 0;
    private int tickCounter = 0;
    private Vec3 lastCheckedPos = null;
    private int stuckTicks = 0;

    private long lastReactedPriorityTimestamp = -1;
    private long lastReactedSoundTimestamp = -1;

    private static final class ResolvedSoundGroup {
        final SoundGroupData data;
        final List<ResourceLocation> locations;
        final boolean isPriority;

        ResolvedSoundGroup(SoundGroupData data) {
            this.data = data;
            this.isPriority = data.groupName().startsWith("auto_priority_");
            List<String> sounds = data.sounds();
            this.locations = new ArrayList<>(sounds.size());
            for (String s : sounds) {
                this.locations.add(ResourceLocation.parse(s));
            }
        }
    }

    public ReactToGeneralSoundGoal(Mob mob, double speed, double range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        this.entityId = Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(mob.getType())).toString();
        this.isMonster = mob instanceof Monster;
        this.resolvedGroups = new ArrayList<>(soundGroups.size());
        for (SoundGroupData group : soundGroups) {
            resolvedGroups.add(new ResolvedSoundGroup(group));
        }
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) return false;

        long now = System.currentTimeMillis();
        expirePrioritySound(now);

        if (lastPrioritySoundPos != null) {
            if (lastPrioritySoundTimestamp == lastReactedPriorityTimestamp) return false;
            Vec3 mobPos = mob.position();
            double effectiveRange = range * 1.5;
            if (isWeatherActive()) effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            if (mobPos.distanceToSqr(lastPrioritySoundPos) <= effectiveRange * effectiveRange) {
                cachedTarget = resolveDestination(mobPos, lastPrioritySoundPos, effectiveRange);
                cachedSpeed = speed * 1.5;
                targetSetTime = now;
                return cachedTarget != null;
            }
        }

        return findSoundTarget(now);
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) return false;
        if (cachedTarget == null) return false;
        if (mob.getNavigation().isDone()) return false;
        if (stuckTicks >= STUCK_MAX_TICKS) return false;

        long now = System.currentTimeMillis();
        expirePrioritySound(now);

        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) <= PRIORITY_SOUND_DURATION_MS) {
            return true;
        }

        return (now - targetSetTime) <= SOUND_REACTION_TIMEOUT;
    }

    @Override
    public void start() {
        tickCounter = 0;
        stuckTicks = 0;
        lastCheckedPos = mob.position();

        if (lastPrioritySoundPos != null) {
            lastReactedPriorityTimestamp = lastPrioritySoundTimestamp;
        }

        if (cachedTarget != null) {
            mob.getNavigation().moveTo(cachedTarget.x, cachedTarget.y, cachedTarget.z, cachedSpeed);
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

        if (isMonster && tickCounter % LOOK_UPDATE_INTERVAL == 0) {
            mob.getLookControl().setLookAt(cachedTarget.x, cachedTarget.y, cachedTarget.z, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        cachedTarget = null;
        cachedSpeed = 0;
        tickCounter = 0;
        stuckTicks = 0;
        lastCheckedPos = null;
        mob.getNavigation().stop();
    }

    private boolean findSoundTarget(long now) {
        Vec3 mobPos = mob.position();

        for (int i = 0; i < resolvedGroups.size(); i++) {
            ResolvedSoundGroup resolved = resolvedGroups.get(i);
            List<ResourceLocation> locations = resolved.locations;
            List<String> sounds = resolved.data.sounds();

            for (int j = 0; j < locations.size(); j++) {
                String soundStr = sounds.get(j);
                if (!GeneralSoundsConfig.canEntityReactToSound(entityId, soundStr)) continue;

                TimedSoundData data = SoundEventTracker.getLastSoundData(locations.get(j));
                if (data == null) continue;
                if (data.timestamp() == lastReactedSoundTimestamp) continue;

                double effectiveRange = range * resolved.data.rangeMultiplier();
                if (isWeatherActive()) effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();

                if (mobPos.distanceToSqr(data.position()) > effectiveRange * effectiveRange) continue;

                Vec3 destination = resolveDestination(mobPos, data.position(), effectiveRange);
                if (destination == null) continue;

                cachedTarget = destination;
                cachedSpeed = speed * resolved.data.speedMultiplier() * (resolved.isPriority ? 1.3 : 1.0);
                targetSetTime = now;
                lastReactedSoundTimestamp = data.timestamp();

                if (resolved.isPriority && lastPrioritySoundPos == null) {
                    setPrioritySound(data.position());
                }

                return true;
            }
        }

        return false;
    }

    private Vec3 resolveDestination(Vec3 mobPos, Vec3 soundPos, double effectiveRange) {
        if (isMonster) {
            return grounded(soundPos);
        } else {
            Vec3 fleeDir = mobPos.subtract(soundPos);
            double length = fleeDir.length();
            if (length < 0.001) return null;
            return grounded(mobPos.add(fleeDir.normalize().scale(effectiveRange)));
        }
    }

    private void expirePrioritySound(long now) {
        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }
    }

    private boolean isWeatherActive() {
        return mob.level.isRaining() || mob.level.isThundering();
    }

    private Vec3 grounded(Vec3 desiredXZ) {
        BlockPos base = new BlockPos(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    public static void setPrioritySound(Vec3 position) {
        lastPrioritySoundPos = position;
        lastPrioritySoundTimestamp = System.currentTimeMillis();
    }
}