package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.Plugin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FollowVoiceGoal extends Goal {

    private final Mob mob;
    private final double speedModifier;
    private final int voiceDetectionRange;
    private Player targetPlayer;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private long timePlayerInRange;
    private final long maxFollowTime;

    public FollowVoiceGoal(Mob mob, double speedModifier, int detectionRange, double threshold, long maxFollowTime) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.maxFollowTime = maxFollowTime;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        targetPlayer = getNearestPlayerInRange();
        targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
        return targetPlayer != null || targetSoundPosition != null;
    }

    @Override
    public void start() {
        if (targetPlayer != null) {
            timePlayerInRange = System.currentTimeMillis();
        } else if (targetSoundPosition != null) {
            moveToSoundPosition();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return targetPlayer != null || (targetSoundPosition != null && !mob.getNavigation().isDone());
    }

    @Override
    public void tick() {
        if (targetPlayer != null) {
            targetSoundPosition = null;
            handlePlayerInteraction();
        } else if (targetSoundPosition != null) {
            handleSoundInteraction();
        }
    }

    @Override
    public void stop() {
        targetSoundPosition = null;
        targetPlayer = null;
        mob.getNavigation().stop();
    }

    private void handlePlayerInteraction() {
        if (targetPlayer.isCreative()) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - timePlayerInRange > maxFollowTime) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        mob.getNavigation().setSpeedModifier(speedModifier);

        if (mob.getTarget() == null) {
            mob.setTarget(targetPlayer);
        }
    }

    private void handleSoundInteraction() {
        double distanceToTarget = mob.blockPosition().distSqr(targetSoundPosition);

        if (distanceToTarget <= 1.5 * 1.5) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
            if (targetSoundPosition != null) {
                moveToSoundPosition();
            } else {
                mob.getNavigation().stop();
            }
            return;
        }

        if (distanceToTarget > (double) (voiceDetectionRange * voiceDetectionRange) / 2) {
            BlockPos newSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
            if (newSoundPosition == null) {
                targetSoundPosition = null;
                mob.getNavigation().stop();
                return;
            } else {
                targetSoundPosition = newSoundPosition;
                moveToSoundPosition();
            }
        }

        double soundSpeed = Plugin.getLastSoundSpeed(mob.blockPosition(), voiceDetectionRange);
        mob.getNavigation().setSpeedModifier(soundSpeed);
    }

    private Player getNearestPlayerInRange() {
        return mob.level.getNearestPlayer(mob, 5);
    }

    private void moveToSoundPosition() {
        if (targetSoundPosition != null) {
            mob.getNavigation().moveTo(
                    targetSoundPosition.getX() + 0.5,
                    targetSoundPosition.getY(),
                    targetSoundPosition.getZ() + 0.5,
                    speedModifier
            );
        }
    }
}