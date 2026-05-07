package com.armilp.ezvcsurvival.compat.guns;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;

import java.util.Map;

public final class PointBlankSoundsConfig {

    public static void apply(Map<String, GeneralSoundsConfig.SoundEntry> sounds) {
        addGroup(sounds, new String[]{
                "pointblank:glock17", "pointblank:m9", "pointblank:m1911a1",
                "pointblank:p30l", "pointblank:deserteagle", "pointblank:rhino"
        }, 1.0, 4.0); // Pistols

        addGroup(sounds, new String[]{
                "pointblank:ak12", "pointblank:m4a1", "pointblank:m4sopmodii", "pointblank:m16a1",
                "pointblank:hk416", "pointblank:scarl_unsilenced", "pointblank:xm7_unsilenced",
                "pointblank:g36c", "pointblank:aug", "pointblank:g41", "pointblank:ak47",
                "pointblank:ak74", "pointblank:an94", "pointblank:ar57", "pointblank:xm29"
        }, 1.0, 6.5); // Rifles

        addGroup(sounds, new String[]{
                "pointblank:mp5", "pointblank:mp7", "pointblank:ro635", "pointblank:ump45_unsilenced",
                "pointblank:vector", "pointblank:p90", "pointblank:m950", "pointblank:tmp",
                "pointblank:sl8"
        }, 1.0, 3.0); // SMG

        addGroup(sounds, new String[]{
                "pointblank:mk14ebr", "pointblank:uar10", "pointblank:g3", "pointblank:wa2000",
                "pointblank:xm3", "pointblank:l96a1", "pointblank:ballista", "pointblank:gm6lynx"
        }, 1.0, 5.0); // Snipers

        addGroup(sounds, new String[]{
                "pointblank:m590", "pointblank:m870", "pointblank:spas12", "pointblank:aa12",
                "pointblank:citoricxs", "pointblank:hs12"
        }, 1.0, 4.8); // Shotguns

        addGroup(sounds, new String[]{
                "pointblank:mgl_shoot", "pointblank:launcher", "pointblank:at4"
        }, 1.0, 10.0); // RPG

        addGroup(sounds, new String[]{
                "pointblank:lamg", "pointblank:mk48", "pointblank:m249", "pointblank:m134minigun"
        }, 1.0, 8.0); // MG
    }

    private static void addGroup(Map<String, GeneralSoundsConfig.SoundEntry> sounds, String[] ids, double speed, double range) {
        for (String id : ids) {
            sounds.put(id, new GeneralSoundsConfig.SoundEntry(true, speed, range));
        }
    }

}
