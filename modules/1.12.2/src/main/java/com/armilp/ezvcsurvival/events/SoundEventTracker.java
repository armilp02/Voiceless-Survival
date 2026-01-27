package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.mixins.GoalSelectorAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventTracker {
    private static final long SOUND_EXPIRATION_MS = 4000;
    private static final Map<ResourceLocation, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<ResourceLocation, TimedSoundData>();

    public static void setLastPlayedPosition(ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        lastPlayedPositions.put(sound, new TimedSoundData(new Vec3d(x, y, z), System.currentTimeMillis(), speedMultiplier, rangeMultiplier));
    }

    public static void notifyNearbyMobs(WorldServer level, ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        Vec3d soundPos = new Vec3d(x, y, z);

        for (Entity entity : level.loadedEntityList) {
            if (!(entity instanceof EntityLiving)) continue;
            EntityLiving mob = (EntityLiving) entity;
            if (mob.getAttackTarget() != null) continue;

            // Usar el accessor para obtener taskEntries
            Set<EntityAITasks.EntityAITaskEntry> taskEntries = ((GoalSelectorAccessor) mob.tasks).getTaskEntries();

            for (EntityAITasks.EntityAITaskEntry taskEntry : taskEntries) {
                EntityAIBase aiTask = taskEntry.action;
                if (aiTask instanceof ReactToGeneralSoundGoal) {
                    ((ReactToGeneralSoundGoal) aiTask).onSoundPlayed(sound, soundPos, speedMultiplier, rangeMultiplier);
                    break;
                }
            }
        }
    }

    public static Vec3d getLastPlayedPositionForSound(ResourceLocation soundLocation) {
        TimedSoundData data = lastPlayedPositions.get(soundLocation);
        if (data != null && (System.currentTimeMillis() - data.timestamp() <= SOUND_EXPIRATION_MS)) {
            return data.position();
        }
        return null;
    }
}