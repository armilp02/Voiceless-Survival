package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.Plugin;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import java.util.EnumSet;

public class RunawayVoiceGoal extends Goal {

    private final AnimalEntity mob;
    private final double speedModifier;
    private final double voiceDetectionRange;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private int ambientSoundCount;
    private int distanceCovered = 0;

    public RunawayVoiceGoal(AnimalEntity mob, double speedModifier, double detectionRange, double threshold) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.ambientSoundCount = 0;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange, threshold);
        return targetSoundPosition != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetSoundPosition != null && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        if (targetSoundPosition != null) {
            Vector3d groundedDanger = grounded(Vector3d.atCenterOf(targetSoundPosition));
            fleeFrom(groundedDanger);
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
        ambientSoundCount = 0;
        mob.getNavigation().stop();
    }

    private void handleSoundThreat() {
        distanceCovered++;

        BlockPos groundedPos = mob.level.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, targetSoundPosition);
        double gx = groundedPos.getX() + 0.5;
        double gz = groundedPos.getZ() + 0.5;

        if (distanceCovered > 10 && distanceCovered % 20 == 0) {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(
                    gx,
                    groundedPos.getY(),
                    gz,
                    30.0F,
                    30.0F
            );
        }

        double dx = mob.getX() - gx;
        double dz = mob.getZ() - gz;
        double distanceSq2D = dx * dx + dz * dz;

        if (targetSoundPosition == null || distanceSq2D > threshold * threshold) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange, threshold);
        } else {
            fleeFrom(new Vector3d(gx, groundedPos.getY(), gz));
        }
    }

    private void fleeFrom(Vector3d dangerPosition) {
        Vector3d fleeDirection = mob.position().subtract(dangerPosition).normalize().scale(20.0);
        double randomOffsetX = (mob.getRandom().nextDouble() - 0.5) * 5.0;
        double randomOffsetZ = (mob.getRandom().nextDouble() - 0.5) * 5.0;

        Vector3d fleeTarget = mob.position().add(fleeDirection).add(randomOffsetX, 0, randomOffsetZ);
        Vector3d groundedTarget = grounded(fleeTarget);

        if (isDangerousBlock(new BlockPos(groundedTarget.x, groundedTarget.y, groundedTarget.z))) {
            fleeDirection = fleeDirection.add(mob.getRandom().nextDouble() * 5.0, 0, mob.getRandom().nextDouble() * 5.0);
            fleeTarget = mob.position().add(fleeDirection);
            groundedTarget = grounded(fleeTarget);
        }

        mob.getNavigation().moveTo(groundedTarget.x, groundedTarget.y, groundedTarget.z, speedModifier);

        if (ambientSoundCount < 2 && mob.getRandom().nextDouble() < 0.5) {
            mob.playAmbientSound();
            ambientSoundCount++;
        }
    }

    private Vector3d grounded(Vector3d desiredXZ) {
        BlockPos base = new BlockPos(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = mob.level.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, base);
        return new Vector3d(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    private boolean isDangerousBlock(BlockPos pos) {
        ServerWorld level = (ServerWorld) mob.level;
        BlockState blockState = level.getBlockState(pos);

        return !blockState.getFluidState().isEmpty() || !blockState.isSolidRender(level, pos);
    }
}