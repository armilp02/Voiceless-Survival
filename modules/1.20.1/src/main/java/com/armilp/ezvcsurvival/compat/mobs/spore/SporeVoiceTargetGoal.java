package com.armilp.ezvcsurvival.compat.mobs.spore;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.Sentities.BaseEntities.UtilityEntity;
import com.armilp.ezvcsurvival.voicechat.Plugin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

public class SporeVoiceTargetGoal extends Goal {

    private final Mob mob;
    private final double speedModifier;
    private final int voiceDetectionRange;
    private Player targetPlayer;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private long timePlayerInRange;
    private final long maxFollowTime;

    public SporeVoiceTargetGoal(Infected infected, double speedModifier, int detectionRange, double threshold, long maxFollowTime) {
        this.mob = infected;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.maxFollowTime = maxFollowTime;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
    }

    public SporeVoiceTargetGoal(Calamity calamity, double speedModifier, int detectionRange, double threshold, long maxFollowTime) {
        this.mob = calamity;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.maxFollowTime = maxFollowTime;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
    }

    public SporeVoiceTargetGoal(UtilityEntity utilityEntity, double speedModifier, int detectionRange, double threshold, long maxFollowTime) {
        this.mob = utilityEntity;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.maxFollowTime = maxFollowTime;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (mob.getTarget() != null) {
            return false;
        }

        targetPlayer = getNearestPlayerInRange();
        targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange, threshold);
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
        if (mob.getTarget() != null) {
            return false;
        }
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
        if (targetPlayer.isCreative() || targetPlayer.isSpectator()) {
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
        BlockPos groundedPos = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetSoundPosition);
        double dx = (mob.getX() - (groundedPos.getX() + 0.5));
        double dz = (mob.getZ() - (groundedPos.getZ() + 0.5));
        double distanceSq = dx * dx + dz * dz;
        double arrivalThresholdSq = this.threshold * this.threshold;

        if (distanceSq <= arrivalThresholdSq) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange, threshold);
            if (targetSoundPosition != null) {
                moveToSoundPosition();
            } else {
                mob.getNavigation().stop();
            }
            return;
        }

        if (distanceSq > (voiceDetectionRange * voiceDetectionRange) / 2.0) {
            BlockPos newSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange, threshold);
            if (newSoundPosition == null) {
                targetSoundPosition = null;
                mob.getNavigation().stop();
                return;
            } else {
                targetSoundPosition = newSoundPosition;
                moveToSoundPosition();
            }
        }
        mob.getNavigation().setSpeedModifier(speedModifier);
    }

    private Player getNearestPlayerInRange() {
        return mob.level().getNearestPlayer(mob, 5);
    }

    private void moveToSoundPosition() {
        if (targetSoundPosition != null) {
            BlockPos ground = mob.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetSoundPosition);
            mob.getNavigation().moveTo(
                    ground.getX() + 0.5,
                    ground.getY(),
                    ground.getZ() + 0.5,
                    speedModifier
            );
        }
    }
}