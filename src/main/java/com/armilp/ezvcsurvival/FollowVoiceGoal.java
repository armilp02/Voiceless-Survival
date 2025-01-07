package com.armilp.ezvcsurvival;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class FollowVoiceGoal extends Goal {

    private final Mob mob;
    private final double speedModifier;
    private final int voiceDetectionRange;

    private BlockPos targetSoundPosition;

    public FollowVoiceGoal(Mob mob, double speedModifier, int detectionRange) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }


    @Override
    public boolean canUse() {
        targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
        return targetSoundPosition != null;
    }

    @Override
    public void start() {
        if (targetSoundPosition != null) {
            moveToSoundPosition();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return targetSoundPosition != null && !mob.getNavigation().isDone();
    }

    @Override
    public void tick() {
        if (targetSoundPosition == null) return;

        double distanceToTarget = mob.blockPosition().distSqr(targetSoundPosition);

        if (distanceToTarget <= 2.0 * 2.0) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
            if (targetSoundPosition != null) {
                moveToSoundPosition();
            } else {
                mob.getNavigation().stop();
            }
        }
    }

    @Override
    public void stop() {
        targetSoundPosition = null;
        mob.getNavigation().stop();
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
