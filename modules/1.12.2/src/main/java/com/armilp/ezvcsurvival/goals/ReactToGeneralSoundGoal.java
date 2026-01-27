package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;

public class ReactToGeneralSoundGoal extends EntityAIBase {
    private static final long PRIORITY_SOUND_DURATION_MS = 3500;
    private static final int NAVIGATION_UPDATE_INTERVAL = 15;
    private static final int LOOK_UPDATE_INTERVAL = 30;
    private static final long SOUND_REACTION_TIMEOUT = 4000;

    public static Vec3d lastPrioritySoundPos = null;
    public static long lastPrioritySoundTimestamp = 0;

    private final EntityCreature mob;
    private final double speed;
    private final double range;
    private final List<SoundGroupData> soundGroups;
    private final String entityId;
    private final boolean isMonster;

    private Vec3d targetSoundPos = null;
    private double targetSpeedMultiplier = 1.0;
    private double targetRangeMultiplier = 1.0;
    private long targetSetTime = 0;
    private int tickCounter = 0;

    public ReactToGeneralSoundGoal(EntityCreature mob, double speed, double range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;

        // Obtener el ID de la entidad
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(mob.getClass().getName()));
        this.entityId = entry != null ? entry.getRegistryName().toString() : "unknown";

        this.isMonster = mob instanceof EntityMob;
        this.setMutexBits(3); // MOVE + TARGET
    }

    public void onSoundPlayed(ResourceLocation soundLoc, Vec3d soundPos, double speedMult, double rangeMult) {
        if (mob.getAttackTarget() != null) return;

        String soundId = soundLoc.toString();
        if (!GeneralSoundsConfig.canEntityReactToSound(entityId, soundId)) return;

        boolean isPriority = false;
        for (int i = 0, size = soundGroups.size(); i < size; i++) {
            SoundGroupData group = soundGroups.get(i);
            if (group.groupName.startsWith("auto_priority_")) {
                if (group.sounds.contains(soundId)) {
                    isPriority = true;
                    speedMult = group.speedMultiplier;
                    rangeMult = group.rangeMultiplier;
                    break;
                }
            }
        }

        if (!isPriority) {
            boolean found = false;
            for (int i = 0, size = soundGroups.size(); i < size; i++) {
                SoundGroupData group = soundGroups.get(i);
                if (group.sounds.contains(soundId)) {
                    speedMult = group.speedMultiplier;
                    rangeMult = group.rangeMultiplier;
                    found = true;
                    break;
                }
            }
            if (!found) return;
        }

        Vec3d mobPos = new Vec3d(mob.posX, mob.posY, mob.posZ);
        double effectiveRange = range * rangeMult;
        if (mob.world.isRaining() || mob.world.isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER;
        }

        double distSq = mobPos.squareDistanceTo(soundPos);
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
    public boolean shouldExecute() {
        if (mob.getAttackTarget() != null) return false;

        long now = System.currentTimeMillis();

        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        if (lastPrioritySoundPos != null) {
            Vec3d mobPos = new Vec3d(mob.posX, mob.posY, mob.posZ);
            double effectiveRange = range * 1.5;
            if (mob.world.isRaining() || mob.world.isThundering()) {
                effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER;
            }
            if (mobPos.squareDistanceTo(lastPrioritySoundPos) <= effectiveRange * effectiveRange) {
                targetSoundPos = lastPrioritySoundPos;
                targetSpeedMultiplier = 1.5;
                targetRangeMultiplier = 1.5;
                return true;
            }
        }

        return targetSoundPos != null && (now - targetSetTime) < SOUND_REACTION_TIMEOUT;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (mob.getAttackTarget() != null) return false;

        long now = System.currentTimeMillis();
        if (targetSoundPos == null) return false;

        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) <= PRIORITY_SOUND_DURATION_MS) {
            return true;
        }

        if ((now - targetSetTime) > SOUND_REACTION_TIMEOUT) return false;

        Vec3d mobPos = new Vec3d(mob.posX, mob.posY, mob.posZ);
        double effectiveRange = range * targetRangeMultiplier;
        if (mob.world.isRaining() || mob.world.isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER;
        }

        return mobPos.squareDistanceTo(targetSoundPos) <= effectiveRange * effectiveRange;
    }

    @Override
    public void startExecuting() {
        tickCounter = 0;
        if (targetSoundPos != null) {
            updateNavigation();
        }
    }

    @Override
    public void updateTask() {
        if (targetSoundPos == null) {
            return;
        }

        tickCounter++;

        if (tickCounter % NAVIGATION_UPDATE_INTERVAL != 0) return;

        updateNavigation();

        if (isMonster && targetSoundPos != null && tickCounter % LOOK_UPDATE_INTERVAL == 0) {
            mob.getLookHelper().setLookPosition(targetSoundPos.x, targetSoundPos.y, targetSoundPos.z, 30.0F, 30.0F);
        }
    }

    @Override
    public void resetTask() {
        targetSoundPos = null;
        tickCounter = 0;
    }

    private void updateNavigation() {
        if (targetSoundPos == null || mob.getAttackTarget() != null) return;

        long now = System.currentTimeMillis();
        if (lastPrioritySoundPos != null && (now - lastPrioritySoundTimestamp) > PRIORITY_SOUND_DURATION_MS) {
            lastPrioritySoundPos = null;
        }

        Vec3d currentPos = new Vec3d(mob.posX, mob.posY, mob.posZ);
        boolean isPriority = targetSoundPos.equals(lastPrioritySoundPos);

        double effectiveRange = range * targetRangeMultiplier;
        double effectiveSpeed = speed * targetSpeedMultiplier;

        if (isPriority) {
            effectiveRange *= 1.5;
            effectiveSpeed *= 1.3;
        }

        if (mob.world.isRaining() || mob.world.isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER;
        }

        double distance = currentPos.distanceTo(targetSoundPos);

        if (distance > effectiveRange) return;

        if (isPriority && distance < 2.0) {
            lastPrioritySoundPos = null;
            targetSoundPos = null;
            return;
        }

        if (distance > 50.0) effectiveSpeed *= 0.8;

        Vec3d target = isMonster ?
                grounded(targetSoundPos) :
                grounded(currentPos.add(currentPos.subtract(targetSoundPos).normalize().scale(effectiveRange)));

        mob.getNavigator().tryMoveToXYZ(target.x, target.y, target.z, effectiveSpeed);
    }

    private Vec3d grounded(Vec3d desiredXZ) {
        BlockPos base = new BlockPos(desiredXZ.x, 0, desiredXZ.z);
        BlockPos top = getGroundPos(base);
        return new Vec3d(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
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

    public static void setPrioritySound(Vec3d position) {
        lastPrioritySoundPos = position;
        lastPrioritySoundTimestamp = System.currentTimeMillis();
    }
}