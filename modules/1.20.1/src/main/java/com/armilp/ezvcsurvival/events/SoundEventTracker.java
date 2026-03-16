package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.data.TimedSoundData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SoundEventTracker {

    private static final long SOUND_EXPIRATION_MS = 4000;
    private static final long DEDUP_WINDOW_MS = 200;
    private static final long CLEANUP_INTERVAL_MS = 8000;
    private static final int MAX_ENTRIES = 256;

    private static final ConcurrentHashMap<ResourceLocation, TimedSoundData> lastPlayedPositions = new ConcurrentHashMap<>(64);
    private static final AtomicLong lastCleanupTime = new AtomicLong(0);

    private SoundEventTracker() {}

    public static void setLastPlayedPosition(ResourceLocation sound, double x, double y, double z,
                                             double speedMultiplier, double rangeMultiplier) {
        long now = System.currentTimeMillis();

        TimedSoundData existing = lastPlayedPositions.get(sound);
        if (existing != null && (now - existing.timestamp()) < DEDUP_WINDOW_MS) {
            return;
        }

        lastPlayedPositions.put(sound, new TimedSoundData(new Vec3(x, y, z), now, speedMultiplier, rangeMultiplier));

        long last = lastCleanupTime.get();
        if ((now - last) > CLEANUP_INTERVAL_MS && lastCleanupTime.compareAndSet(last, now)) {
            cleanup(now);
        }
    }

    public static Vec3 getLastPlayedPositionForSound(ResourceLocation soundLocation) {
        TimedSoundData data = lastPlayedPositions.get(soundLocation);
        if (data != null && (System.currentTimeMillis() - data.timestamp()) <= SOUND_EXPIRATION_MS) {
            return data.position();
        }
        return null;
    }

    public static TimedSoundData getLastSoundData(ResourceLocation soundLocation) {
        TimedSoundData data = lastPlayedPositions.get(soundLocation);
        if (data != null && (System.currentTimeMillis() - data.timestamp()) <= SOUND_EXPIRATION_MS) {
            return data;
        }
        return null;
    }

    private static void cleanup(long now) {
        if (lastPlayedPositions.size() > MAX_ENTRIES) {
            lastPlayedPositions.clear();
            return;
        }
        lastPlayedPositions.entrySet().removeIf(e -> (now - e.getValue().timestamp()) > SOUND_EXPIRATION_MS);
    }
}