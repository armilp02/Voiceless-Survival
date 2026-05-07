package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventTracker {
    private static final Map<ResourceLocation, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<>();

    public static void setLastPlayedPosition(ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        lastPlayedPositions.put(sound, new TimedSoundData(new Vec3(x, y, z), System.currentTimeMillis(), speedMultiplier, rangeMultiplier));
    }

    public static void notifyNearbyMobs(ServerLevel level, ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        Vec3 soundPos = new Vec3(x, y, z);

        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob)) continue;
            if (mob.getTarget() != null) continue;

            for (WrappedGoal wrappedGoal : mob.goalSelector.getAvailableGoals()) {
                if (wrappedGoal.getGoal() instanceof ReactToGeneralSoundGoal) {
                    ((ReactToGeneralSoundGoal) wrappedGoal.getGoal()).onSoundPlayed(sound, soundPos, speedMultiplier, rangeMultiplier);
                    break;
                }
            }
        }
    }
}