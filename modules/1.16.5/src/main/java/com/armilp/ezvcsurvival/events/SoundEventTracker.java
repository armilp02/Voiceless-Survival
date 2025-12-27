package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.mixins.GoalSelectorAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventTracker {
    private static final long SOUND_EXPIRATION_MS = 4000;
    private static final Map<ResourceLocation, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<ResourceLocation, TimedSoundData>();

    public static void setLastPlayedPosition(ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        lastPlayedPositions.put(sound, new TimedSoundData(new Vector3d(x, y, z), System.currentTimeMillis(), speedMultiplier, rangeMultiplier));
    }

    public static void notifyNearbyMobs(ServerWorld level, ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        Vector3d soundPos = new Vector3d(x, y, z);

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof MobEntity)) continue;
            MobEntity mob = (MobEntity) entity;
            if (mob.getTarget() != null) continue;

            // Usar el accessor para obtener availableGoals
            Set<PrioritizedGoal> availableGoals = ((GoalSelectorAccessor) mob.goalSelector).getAvailableGoals();

            for (PrioritizedGoal wrappedGoal : availableGoals) {
                if (wrappedGoal.getGoal() instanceof ReactToGeneralSoundGoal) {
                    ((ReactToGeneralSoundGoal) wrappedGoal.getGoal()).onSoundPlayed(sound, soundPos, speedMultiplier, rangeMultiplier);
                    break;
                }
            }
        }
    }

    public static Vector3d getLastPlayedPositionForSound(ResourceLocation soundLocation) {
        TimedSoundData data = lastPlayedPositions.get(soundLocation);
        if (data != null && (System.currentTimeMillis() - data.timestamp() <= SOUND_EXPIRATION_MS)) {
            return data.position();
        }
        return null;
    }
}