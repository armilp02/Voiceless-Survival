package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.Plugin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.EnumSet;

public class RunawayVoiceGoal extends Goal {

    private final AnimalEntity mob;
    private final double speedModifier;
    private final int voiceDetectionRange;
    private PlayerEntity targetPlayer;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private double targetSoundSpeed;
    private int ambientSoundCount;
    private int fleeTicks = 0;
    private int distanceCovered = 0;

    public RunawayVoiceGoal(AnimalEntity mob, double speedModifier, int detectionRange, double threshold) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.ambientSoundCount = 0;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        targetPlayer = findNearestPlayer();
        targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
        targetSoundSpeed = Plugin.getLastSoundSpeed(mob.blockPosition(), voiceDetectionRange);
        return targetSoundPosition != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetSoundPosition != null && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (targetSoundPosition != null) {
            fleeFrom(Vector3d.atCenterOf(targetSoundPosition));
        }
    }

    @Override
    public void tick() {
        if (targetSoundPosition != null) {
            handleSoundThreat();
        }
    }

    @Override
    public void stop() {
        targetSoundPosition = null;
        targetPlayer = null;
        ambientSoundCount = 0;
        mob.getNavigation().stop();
    }

    private PlayerEntity findNearestPlayer() {
        return (mob.level).getNearestPlayer(mob, voiceDetectionRange);
    }

    private void handleSoundThreat() {
        distanceCovered++;
        if (distanceCovered > 10 && distanceCovered % 20 == 0) { // Pausa cada 10 bloques
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(Vector3d.atCenterOf(targetSoundPosition).x, Vector3d.atCenterOf(targetSoundPosition).y, Vector3d.atCenterOf(targetSoundPosition).z, 30.0F, 30.0F); // Mirar hacia el sonido
        }

        if (targetSoundPosition == null || mob.blockPosition().distSqr(targetSoundPosition) > threshold * threshold) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
            targetSoundSpeed = Plugin.getLastSoundSpeed(mob.blockPosition(), voiceDetectionRange);
        } else {
            fleeFrom(Vector3d.atCenterOf(targetSoundPosition));
        }
    }

    private void fleeFrom(Vector3d dangerPosition) {
        fleeTicks++;

        Vector3d fleeDirection = mob.position().subtract(dangerPosition).normalize().scale(20.0);
        double randomOffsetX = (mob.getRandom().nextDouble() - 0.5) * 5.0;
        double randomOffsetZ = (mob.getRandom().nextDouble() - 0.5) * 5.0;

        Vector3d fleeTarget = mob.position().add(fleeDirection).add(randomOffsetX, 0, randomOffsetZ);
        if (isDangerousBlock(new BlockPos((int) fleeTarget.x, (int) fleeTarget.y, (int) fleeTarget.z))) {
            fleeDirection = fleeDirection.add(mob.getRandom().nextDouble() * 5.0, 0, mob.getRandom().nextDouble() * 5.0);
            fleeTarget = mob.position().add(fleeDirection);
        }

        mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, speedModifier);

        // Make the mob play a panic sound (if applicable), but limit to 2-4 times
        if (ambientSoundCount < 2 && mob.getRandom().nextDouble() < 0.5) { // 50% chance per flee action
            mob.playAmbientSound();
            ambientSoundCount++;
        }
    }

    private boolean isDangerousBlock(BlockPos pos) {
        ServerWorld level = (ServerWorld) mob.level;
        BlockState blockState = level.getBlockState(pos);

        // Verifica si el bloque tiene un fluido o si no es completamente sólido
        return !blockState.getFluidState().isEmpty() || !blockState.isSolidRender(level, pos);
    }
}
