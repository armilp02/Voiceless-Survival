package com.armilp.ezvcsurvival.voicechat.client;

public class ClientVoiceLevel {

    static final double MIN_DB = -45.0;
    static final double MAX_DB = -5.0;
    private static final long STALE_MS = 300;

    private static volatile double level = 0.0;
    private static volatile long lastPacketTime = 0L;

    public static void update(double db) {
        double normalized = (db - MIN_DB) / (MAX_DB - MIN_DB);
        normalized = Math.max(0.0, Math.min(1.0, normalized));
        level = level * 0.35 + normalized * 0.65;
        lastPacketTime = System.currentTimeMillis();
    }

    public static double getLevel() {
        if (System.currentTimeMillis() - lastPacketTime > STALE_MS) {
            level *= 0.85;
            if (level < 0.01) level = 0.0;
        }
        return level;
    }
}