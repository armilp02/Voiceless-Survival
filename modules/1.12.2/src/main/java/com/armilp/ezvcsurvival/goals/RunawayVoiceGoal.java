package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.Plugin;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class RunawayVoiceGoal extends EntityAIBase {

    private final EntityAnimal mob;
    private final double speedModifier;
    private final double voiceDetectionRange;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private int ambientSoundCount;
    private int distanceCovered = 0;

    public RunawayVoiceGoal(EntityAnimal mob, double speedModifier, double detectionRange, double threshold) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.ambientSoundCount = 0;
        this.setMutexBits(3); // MOVE + TARGET
    }

    @Override
    public boolean shouldExecute() {
        targetSoundPosition = Plugin.getLastSoundLocation(mob.getPosition(), voiceDetectionRange, threshold);
        return targetSoundPosition != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return targetSoundPosition != null && !mob.getNavigator().noPath();
    }

    @Override
    public void startExecuting() {
        if (targetSoundPosition != null) {
            Vec3d groundedDanger = grounded(new Vec3d(
                    targetSoundPosition.getX() + 0.5,
                    targetSoundPosition.getY(),
                    targetSoundPosition.getZ() + 0.5
            ));
            fleeFrom(groundedDanger);
        }
    }

    @Override
    public void updateTask() {
        if (targetSoundPosition != null) {
            handleSoundThreat();
        }
    }

    @Override
    public void resetTask() {
        targetSoundPosition = null;
        ambientSoundCount = 0;
        mob.getNavigator().clearPath();
    }

    private void handleSoundThreat() {
        distanceCovered++;

        BlockPos groundedPos = getGroundPos(targetSoundPosition);
        double gx = groundedPos.getX() + 0.5;
        double gz = groundedPos.getZ() + 0.5;

        if (distanceCovered > 10 && distanceCovered % 20 == 0) {
            mob.getNavigator().clearPath();
            mob.getLookHelper().setLookPosition(
                    gx,
                    groundedPos.getY(),
                    gz,
                    30.0F,
                    30.0F
            );
        }

        double dx = mob.posX - gx;
        double dz = mob.posZ - gz;
        double distanceSq2D = dx * dx + dz * dz;

        if (targetSoundPosition == null || distanceSq2D > threshold * threshold) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.getPosition(), voiceDetectionRange, threshold);
        } else {
            fleeFrom(new Vec3d(gx, groundedPos.getY(), gz));
        }
    }

    private void fleeFrom(Vec3d dangerPosition) {
        Vec3d mobPos = new Vec3d(mob.posX, mob.posY, mob.posZ);
        Vec3d fleeDirection = mobPos.subtract(dangerPosition).normalize().scale(20.0);

        double randomOffsetX = (mob.getRNG().nextDouble() - 0.5) * 5.0;
        double randomOffsetZ = (mob.getRNG().nextDouble() - 0.5) * 5.0;

        Vec3d fleeTarget = mobPos.add(fleeDirection).addVector(randomOffsetX, 0, randomOffsetZ);
        Vec3d groundedTarget = grounded(fleeTarget);

        if (isDangerousBlock(new BlockPos(groundedTarget.x, groundedTarget.y, groundedTarget.z))) {
            fleeDirection = fleeDirection.addVector(
                    mob.getRNG().nextDouble() * 5.0,
                    0,
                    mob.getRNG().nextDouble() * 5.0
            );
            fleeTarget = mobPos.add(fleeDirection);
            groundedTarget = grounded(fleeTarget);
        }

        mob.getNavigator().tryMoveToXYZ(groundedTarget.x, groundedTarget.y, groundedTarget.z, speedModifier);

        if (ambientSoundCount < 2 && mob.getRNG().nextDouble() < 0.5) {
            mob.playLivingSound();
            ambientSoundCount++;
        }
    }

    private Vec3d grounded(Vec3d desiredXZ) {
        BlockPos base = new BlockPos(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = getGroundPos(base);
        return new Vec3d(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
    }

    private boolean isDangerousBlock(BlockPos pos) {
        WorldServer level = (WorldServer) mob.world;
        IBlockState blockState = level.getBlockState(pos);

        return blockState.getMaterial().isLiquid() || !blockState.isOpaqueCube();
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