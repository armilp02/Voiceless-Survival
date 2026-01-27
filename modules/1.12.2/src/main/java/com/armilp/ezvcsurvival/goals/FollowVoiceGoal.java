package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.Plugin;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FollowVoiceGoal extends EntityAIBase {

    private final EntityCreature mob;
    private final double speedModifier;
    private final int voiceDetectionRange;
    private EntityPlayer targetPlayer;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private long timePlayerInRange;
    private final long maxFollowTime;

    public FollowVoiceGoal(EntityCreature mob, double speedModifier, int detectionRange, double threshold, long maxFollowTime) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.maxFollowTime = maxFollowTime;
        this.setMutexBits(3); // MOVE + TARGET
    }

    @Override
    public boolean shouldExecute() {
        if (mob.getAttackTarget() != null) {
            return false;
        }

        targetPlayer = getNearestPlayerInRange();
        targetSoundPosition = Plugin.getLastSoundLocation(mob.getPosition(), voiceDetectionRange, threshold);
        return targetPlayer != null || targetSoundPosition != null;
    }

    @Override
    public void startExecuting() {
        if (targetPlayer != null) {
            timePlayerInRange = System.currentTimeMillis();
        } else if (targetSoundPosition != null) {
            moveToSoundPosition();
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (mob.getAttackTarget() != null) {
            return false;
        }
        return targetPlayer != null || (targetSoundPosition != null && !mob.getNavigator().noPath());
    }

    @Override
    public void updateTask() {
        if (targetPlayer != null) {
            targetSoundPosition = null;
            handlePlayerInteraction();
        } else if (targetSoundPosition != null) {
            handleSoundInteraction();
        }
    }

    @Override
    public void resetTask() {
        targetSoundPosition = null;
        targetPlayer = null;
        mob.getNavigator().clearPath();
    }

    private void handlePlayerInteraction() {
        if (targetPlayer.isCreative() || targetPlayer.isSpectator()) {
            targetPlayer = null;
            mob.getNavigator().clearPath();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - timePlayerInRange > maxFollowTime) {
            targetPlayer = null;
            mob.getNavigator().clearPath();
            return;
        }

        mob.getNavigator().setSpeed(speedModifier);

        if (mob.getAttackTarget() == null && mob instanceof EntityLiving) {
            mob.setAttackTarget(targetPlayer);
        }
    }

    private void handleSoundInteraction() {
        BlockPos groundedPos = getGroundPos(targetSoundPosition);
        double dx = (mob.posX - (groundedPos.getX() + 0.5));
        double dz = (mob.posZ - (groundedPos.getZ() + 0.5));
        double distanceSq = dx * dx + dz * dz;
        double arrivalThresholdSq = this.threshold * this.threshold;

        if (distanceSq <= arrivalThresholdSq) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.getPosition(), voiceDetectionRange, threshold);
            if (targetSoundPosition != null) {
                moveToSoundPosition();
            } else {
                mob.getNavigator().clearPath();
            }
            return;
        }

        if (distanceSq > (voiceDetectionRange * voiceDetectionRange) / 2.0) {
            BlockPos newSoundPosition = Plugin.getLastSoundLocation(mob.getPosition(), voiceDetectionRange, threshold);
            if (newSoundPosition == null) {
                targetSoundPosition = null;
                mob.getNavigator().clearPath();
                return;
            } else {
                targetSoundPosition = newSoundPosition;
                moveToSoundPosition();
            }
        }
        mob.getNavigator().setSpeed(speedModifier);
    }

    private EntityPlayer getNearestPlayerInRange() {
        return mob.world.getClosestPlayerToEntity(mob, 5.0);
    }

    private void moveToSoundPosition() {
        if (targetSoundPosition != null) {
            BlockPos ground = getGroundPos(targetSoundPosition);
            mob.getNavigator().tryMoveToXYZ(
                    ground.getX() + 0.5,
                    ground.getY(),
                    ground.getZ() + 0.5,
                    speedModifier
            );
        }
    }

    private BlockPos getGroundPos(BlockPos pos) {
        World world = mob.world;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(pos);

        // Buscar hacia arriba hasta encontrar aire
        while (mutablePos.getY() < 256 && world.getBlockState(mutablePos).isOpaqueCube()) {
            mutablePos.setY(mutablePos.getY() + 1);
        }

        // Buscar hacia abajo hasta encontrar un bloque sólido
        while (mutablePos.getY() > 0 && !world.getBlockState(mutablePos.down()).isOpaqueCube()) {
            mutablePos.setY(mutablePos.getY() - 1);
        }

        return mutablePos.toImmutable();
    }
}