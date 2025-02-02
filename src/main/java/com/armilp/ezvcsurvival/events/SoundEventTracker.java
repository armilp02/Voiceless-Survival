package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventTracker {
    // Time (in ms) during which the sound record is considered "active" (3 seconds)
    private static final long SOUND_EXPIRATION_MS = 3000;

    // We use ConcurrentHashMap to allow concurrent modifications without exceptions
    private static final Map<ResourceLocation, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<>();

    public static void registerSound(ResourceLocation soundLocation, Vec3 position) {
        long now = System.currentTimeMillis();
        lastPlayedPositions.put(soundLocation, new TimedSoundData(position, now));
    }

    public static Vec3 getLastPlayedPositionForAny(List<ResourceLocation> soundLocations) {
        long now = System.currentTimeMillis();
        cleanupExpired(now);
        for (ResourceLocation loc : soundLocations) {
            TimedSoundData event = lastPlayedPositions.get(loc);
            if (event != null) {
                return event.position;
            }
        }
        return null;
    }

    // Removes expired entries from the map.
    private static void cleanupExpired(long currentTime) {
        lastPlayedPositions.entrySet().removeIf(entry ->
                currentTime - entry.getValue().timestamp > SOUND_EXPIRATION_MS);
    }
}
