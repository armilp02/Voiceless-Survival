package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ReactToGeneralSoundGoal extends Goal {
    private static final long PRIORITY_SOUND_DURATION_MS = 3500;
    private static final int MAX_CONCURRENT_REACTIONS = 30;
    private static final int NAVIGATION_UPDATE_INTERVAL = 15;
    private static final int LOOK_UPDATE_INTERVAL = 30;

    private static final Map<String, ResourceLocation> LOCATION_CACHE = new ConcurrentHashMap<>(128);
    private static final AtomicInteger activeMobsReacting = new AtomicInteger(0);

    public static Vec3 lastPrioritySoundPos = null;
    public static long lastPrioritySoundTimestamp = 0;

    private final Mob mob;
    private final double speed;
    private final double range;
    private final List<SoundGroupData> soundGroups;
    private final String entityId;
    private final boolean isMonster;

    private Vec3 targetSoundPos = null;
    private double targetSpeedMultiplier = 1.0;
    private double targetRangeMultiplier = 1.0;
    private long targetSetTime = 0;
    private int tickCounter = 0;
    private boolean isReacting = false;

    public ReactToGeneralSoundGoal(Mob mob, double speed, double range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        this.entityId = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType())).toString();
        this.isMonster = mob instanceof Monster;
    }

    public void onSoundPlayed(ResourceLocation soundLoc, Vec3 soundPos, double speedMult, double rangeMult) {
        if (mob.getTarget() != null) return;

        String soundId = soundLoc.toString();
        if (!GeneralSoundsConfig.canEntityReactToSound(entityId, soundId)) return;

        boolean isPriority = false;
        for (int i = 0, size = soundGroups.size(); i < size; i++) {
            SoundGroupData group = soundGroups.get(i);
            if (group.groupName().startsWith("auto_priority_")) {
                if (group.sounds().contains(soundId)) {
                    isPriority = true;
                    speedMult = group.speedMultiplier();
                    rangeMult = group.rangeMultiplier();
                    break;
                }
            }
        }

        if (!isPriority) {
            boolean found = false;
            for (int i = 0, size = soundGroups.size(); i < size; i++) {
                SoundGroupData group = soundGroups.get(i);
                if (group.sounds().contains(soundId)) {
                    speedMult = group.speedMultiplier();
                    rangeMult = group.rangeMultiplier();
                    found = true;
                    break;
                }
            }
            if (!found) return;
        }

        Vec3 mobPos = mob.position();
        double effectiveRange = range * rangeMult;
        if (mob.level().isRaining() || mob.level().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        double distSq = mobPos.distanceToSqr(soundPos);
        if (distSq > effectiveRange * effectiveRange) return;

        this.targetSoundPos = soundPos;
        this.targetSpeedMultiplier = speedMult;
        this.targetRangeMultiplier = rangeMult;
        this.targetSetTime = System.currentTimeMillis();

        if (isPriority && lastPrioritySoundPos == null) {
            setPrioritySound(soundPos);
        }
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) return false;
        if (!isReacting && activeMobsReacting.get() >= MAX_CONCURRENT_REACTIONS) return false;

        long now = System.currentTimeMillis();

        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        if (lastPrioritySoundPos != null) {
            Vec3 mobPos = mob.position();
            double effectiveRange = range * 1.5;
            if (mob.level().isRaining() || mob.level().isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }
            if (mobPos.distanceToSqr(lastPrioritySoundPos) <= effectiveRange * effectiveRange) {
                targetSoundPos = lastPrioritySoundPos;
                targetSpeedMultiplier = 1.5;
                targetRangeMultiplier = 1.5;
                return true;
            }
        }

        if (targetSoundPos != null && (now - targetSetTime) < 4000) {
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getTarget() != null) return false;

        long now = System.currentTimeMillis();
        if (targetSoundPos == null) return false;

        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) <= PRIORITY_SOUND_DURATION_MS) {
            return true;
        }

        if ((now - targetSetTime) > 4000) return false;

        Vec3 mobPos = mob.position();
        double effectiveRange = range * targetRangeMultiplier;
        if (mob.level().isRaining() || mob.level().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        return mobPos.distanceToSqr(targetSoundPos) <= effectiveRange * effectiveRange;
    }

    @Override
    public void start() {
        if (!isReacting) {
            activeMobsReacting.incrementAndGet();
            isReacting = true;
        }
        tickCounter = 0;
        updateNavigation();
    }

    @Override
    public void tick() {
        if (targetSoundPos == null) return;

        tickCounter++;

        if (tickCounter % NAVIGATION_UPDATE_INTERVAL != 0) return;

        updateNavigation();

        if (isMonster && tickCounter % LOOK_UPDATE_INTERVAL == 0) {
            mob.getLookControl().setLookAt(targetSoundPos.x, targetSoundPos.y, targetSoundPos.z, 30.0F, 30.0F);
        }
    }

    @Override
    public void stop() {
        if (isReacting) {
            activeMobsReacting.decrementAndGet();
            isReacting = false;
        }
        targetSoundPos = null;
        tickCounter = 0;
    }

    private void updateNavigation() {
        if (mob.getTarget() != null || targetSoundPos == null) return;

        long now = System.currentTimeMillis();
        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        Vec3 currentPos = mob.position();
        boolean isPriority = lastPrioritySoundPos != null && targetSoundPos.equals(lastPrioritySoundPos);

        double effectiveRange = range * targetRangeMultiplier;
        double effectiveSpeed = speed * targetSpeedMultiplier;

        if (isPriority) {
            effectiveRange *= 1.5;
            effectiveSpeed *= 1.3;
        }

        if (mob.level().isRaining() || mob.level().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        double distance = currentPos.distanceTo(targetSoundPos);

        if (distance > effectiveRange) return;

        if (isPriority && distance < 2.0) {
            lastPrioritySoundPos = null;
            targetSoundPos = null;
            return;
        }

        if (distance > 50.0) effectiveSpeed *= 0.8;

        Vec3 target = isMonster ?
                grounded(targetSoundPos) :
                grounded(currentPos.add(currentPos.subtract(targetSoundPos).normalize().scale(effectiveRange)));

        mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
    }

    private Vec3 grounded(Vec3 desiredXZ) {
        BlockPos base = BlockPos.containing(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    public static void setPrioritySound(Vec3 position) {
        lastPrioritySoundPos = position;
        lastPrioritySoundTimestamp = System.currentTimeMillis();
    }
}