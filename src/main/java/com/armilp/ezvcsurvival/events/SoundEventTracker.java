package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventTracker {
    private static final long SOUND_EXPIRATION_MS = 4000;
    private static final Map<ResourceLocation, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<>();

    public static void setLastPlayedPosition(ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        lastPlayedPositions.put(sound, new TimedSoundData(new Vec3(x, y, z), System.currentTimeMillis(), speedMultiplier, rangeMultiplier));
    }

    public static Vec3 getLastPlayedPositionForSound(ResourceLocation soundLocation) {
        long now = System.currentTimeMillis();
        TimedSoundData data = lastPlayedPositions.get(soundLocation);
        if (data != null && (now - data.timestamp <= SOUND_EXPIRATION_MS)) {
            return data.position;
        }
        return null;
    }
}
