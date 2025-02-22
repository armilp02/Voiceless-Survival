package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEventTracker {
    private static final long SOUND_EXPIRATION_MS = 3000;
    private static final Map<ResourceLocation, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<>();

    public static void registerSound(ResourceLocation soundLocation, Vec3 position) {
        long now = System.currentTimeMillis();
        lastPlayedPositions.put(soundLocation, new TimedSoundData(position, now));
    }

    public static void setLastPlayedPosition(ResourceLocation sound, double x, double y, double z) {
        lastPlayedPositions.put(sound, new TimedSoundData(new Vec3(x, y, z), System.currentTimeMillis()));
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
