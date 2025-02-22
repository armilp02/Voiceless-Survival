package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventTracker {
    private static final long SOUND_EXPIRATION_MS = 3000;
    private static final Map<ResourceLocation, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<>();

    public static void registerSound(ResourceLocation soundLocation, Vector3d position) {
        long now = System.currentTimeMillis();
        lastPlayedPositions.put(soundLocation, new TimedSoundData(position, now));
    }

    public static Vector3d getLastPlayedPositionForSound(ResourceLocation soundLocation) {
        long now = System.currentTimeMillis();
        TimedSoundData data = lastPlayedPositions.get(soundLocation);
        if (data != null && (now - data.timestamp <= SOUND_EXPIRATION_MS)) {
            return data.position;
        }
        return null;
    }
}
