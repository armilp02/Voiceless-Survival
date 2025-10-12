package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReactToGeneralSoundGoal extends Goal {
    private final Mob mob;
    private final double speed;
    private final double range;
    private final List<SoundGroupData> soundGroups;

    private Vec3 lastAttackerPos = null;
    private static final List<ReactToGeneralSoundGoal> activeGoals = new CopyOnWriteArrayList<>();

    public static Vec3 lastPrioritySoundPos = null;
    public static long lastPrioritySoundTimestamp = 0;
    private static final long PRIORITY_SOUND_DURATION_MS = 3500;

    public ReactToGeneralSoundGoal(Mob mob, double speed, double range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        activeGoals.add(this);
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) {
            return false;
        }

        if (lastPrioritySoundPos != null &&
                System.currentTimeMillis() - lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        checkForPrioritySounds();

        Vec3 mobCenterPos = mob.position();
        double effectiveRange = range;
        if (mob.level().isRaining() || mob.level().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        if (lastPrioritySoundPos != null) {
            double priorityRange = effectiveRange * 1.5;
            if (mobCenterPos.distanceTo(lastPrioritySoundPos) <= priorityRange) {
                return true;
            }
        }

        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds) {
                ResourceLocation loc = toLocation(soundStr);
                Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(loc);
                if (pos != null) {
                    double grpRange = range * group.rangeMultiplier;
                    if (mob.level().isRaining() || mob.level().isThundering()) {
                        grpRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
                    }
                    if (mobCenterPos.distanceTo(pos) <= grpRange) {
                        return true;
                    }
                }
            }
        }

        boolean hurtTriggered = !(mob instanceof Monster) && lastAttackerPos != null;
        return hurtTriggered;
    }

    @Override
    public void start() {
        updateNavigation();
    }

    @Override
    public void tick() {
        updateNavigation();

        if (mob instanceof Monster) {
            Vec3 targetPos = getCurrentTargetPosition();
            if (targetPos != null) {
                mob.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z, 30.0F, 30.0F);
            }
        }
    }

    @Override
    public void stop() {
        activeGoals.remove(this);
    }

    private void checkForPrioritySounds() {
        for (SoundGroupData priority : SoundConfig.getPriorityGroups()) {
            for (String soundStr : priority.sounds) {
                ResourceLocation soundLoc = toLocation(soundStr);
                Vec3 priorityPos = SoundEventTracker.getLastPlayedPositionForSound(soundLoc);
                if (priorityPos != null) {
                    lastPrioritySoundPos = priorityPos;
                    lastPrioritySoundTimestamp = System.currentTimeMillis();
                    return;
                }
            }
        }
    }

    private void updateNavigation() {
        if (mob.getTarget() != null) {
            return;
        }

        if (lastPrioritySoundPos != null &&
                System.currentTimeMillis() - lastPrioritySoundTimestamp > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        checkForPrioritySounds();

        Vec3 currentPos = mob.position();

        if (lastPrioritySoundPos != null) {
            double priorityRange = range * 1.5;
            double prioritySpeed = speed * 1.3;

            if (mob.level().isRaining() || mob.level().isThundering()) {
                priorityRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }

            if (currentPos.distanceTo(lastPrioritySoundPos) <= priorityRange) {
                Vec3 target;
                if (mob instanceof Monster) {
                    target = grounded(lastPrioritySoundPos);
                } else {
                    Vec3 directionAway = currentPos.subtract(lastPrioritySoundPos).normalize();
                    target = grounded(currentPos.add(directionAway.scale(priorityRange)));
                }

                if (currentPos.distanceTo(lastPrioritySoundPos) > 50.0) {
                    mob.getNavigation().moveTo(target.x, target.y, target.z, prioritySpeed * 0.8);
                } else {
                    mob.getNavigation().moveTo(target.x, target.y, target.z, prioritySpeed);
                }

                if (currentPos.distanceTo(lastPrioritySoundPos) < 2.0) {
                    lastPrioritySoundPos = null;
                }
                return;
            }
        }

        if (lastPrioritySoundPos == null) {
            for (SoundGroupData group : soundGroups) {
                for (String soundStr : group.sounds) {
                    ResourceLocation loc = toLocation(soundStr);
                    Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(loc);
                    if (pos != null) {
                        double grpRange = range * group.rangeMultiplier;
                        double grpSpeed = speed * group.speedMultiplier;
                        if (mob.level().isRaining() || mob.level().isThundering()) {
                            grpRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
                        }

                        if (currentPos.distanceTo(pos) <= grpRange) {
                            if (mob instanceof Monster) {
                                Vec3 target = grounded(pos);
                                if (currentPos.distanceTo(pos) > 50.0) {
                                    mob.getNavigation().moveTo(target.x, target.y, target.z, grpSpeed * 0.8);
                                } else {
                                    mob.getNavigation().moveTo(target.x, target.y, target.z, grpSpeed);
                                }
                            } else {
                                Vec3 directionAway = currentPos.subtract(pos).normalize();
                                Vec3 fleeTarget = grounded(currentPos.add(directionAway.scale(grpRange)));
                                mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, grpSpeed);
                            }
                            return;
                        }
                    }
                }
            }

            if (!(mob instanceof Monster) && lastAttackerPos != null) {
                Vec3 directionAway = currentPos.subtract(lastAttackerPos).normalize();
                Vec3 fleeTarget = grounded(currentPos.add(directionAway.scale(range)));

                if (currentPos.distanceTo(lastAttackerPos) > 50.0) {
                    mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, speed * 0.8);
                } else {
                    mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, speed);
                }
            }
        }
    }

    private ResourceLocation toLocation(String soundStr) {
        if (soundStr.contains(":")) {
            return ResourceLocation.parse(soundStr);
        }
        return ResourceLocation.withDefaultNamespace(soundStr);
    }

    private Vec3 grounded(Vec3 desiredXZ) {
        BlockPos base = BlockPos.containing(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vec3(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    private Vec3 getCurrentTargetPosition() {
        if (lastPrioritySoundPos != null) {
            return lastPrioritySoundPos;
        }

        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds) {
                ResourceLocation loc = toLocation(soundStr);
                Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(loc);
                if (pos != null) {
                    double grpRange = range * group.rangeMultiplier;
                    if (mob.level().isRaining() || mob.level().isThundering()) {
                        grpRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
                    }
                    if (mob.position().distanceTo(pos) <= grpRange) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    public static void setPrioritySound(Vec3 position) {
        lastPrioritySoundPos = position;
        lastPrioritySoundTimestamp = System.currentTimeMillis();
    }
}