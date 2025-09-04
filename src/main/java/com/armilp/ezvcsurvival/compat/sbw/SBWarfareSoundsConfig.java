package com.armilp.ezvcsurvival.compat.sbw;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;

import java.util.Map;

public final class SBWarfareSoundsConfig {


    public static void apply(Map<String, GeneralSoundsConfig.SoundEntry> sounds) {
        addGroup(sounds, new String[]{
                "superbwarfare:glock_17_fire_1p", "superbwarfare:taser_fire_1p", "superbwarfare:mp_443_fire_1p",
                "superbwarfare:m_1911_fire_1p", "superbwarfare:trachelium_fire_1p"
        }, 1.0, 4.0);

        addGroup(sounds, new String[]{
                "superbwarfare:ak_12_fire_1p", "superbwarfare:ak_47_fire_1p", "superbwarfare:sks_fire_1p",
                "superbwarfare:m_4_fire_1p", "superbwarfare:hk_416_fire_1p", "superbwarfare:qbz_191_fire_1p",
                "superbwarfare:insidious_fire_1p", "superbwarfare:mk_14_fire_1p", "superbwarfare:marlin_fire_1p"
        }, 1.0, 6.5);

        addGroup(sounds, new String[]{
                "superbwarfare:mp_5_fire_1p", "superbwarfare:vector_fire_1p"
        }, 1.0, 3.0);

        addGroup(sounds, new String[]{
                "superbwarfare:k_98_fire_1p", "superbwarfare:mosin_nagant_fire_1p", "superbwarfare:svd_fire_1p",
                "superbwarfare:awm_fire_1p", "superbwarfare:m_98b_fire_1p", "superbwarfare:sentinel_fire_1p",
                "superbwarfare:hunting_rifle_fire_1p", "superbwarfare:ntw_20_fire_1p"
        }, 1.0, 5.0);

        addGroup(sounds, new String[]{
                "superbwarfare:m_870_fire_1p", "superbwarfare:aa_12_fire_1p"
        }, 1.0, 4.8);

        addGroup(sounds, new String[]{
                "superbwarfare:m_79_fire_1p", "superbwarfare:secondary_cataclysm_fire_1p", "superbwarfare:rpg_fire_1p",
                "superbwarfare:javelin_fire_1p"
        }, 1.0, 10.0);

        addGroup(sounds, new String[]{
                "superbwarfare:devotion_fire_1p", "superbwarfare:rpk_fire_1p", "superbwarfare:m_60_fire_1p",
                "superbwarfare:m_2_hb_fire_1p", "superbwarfare:minigun_fire_1p"
        }, 1.0, 8.0);

        addGroup(sounds, new String[]{
                "superbwarfare:aurelia_sceptre_fire_1p", "superbwarfare:bocek_zoom_fire_1p"
        }, 1.0, 2.0);

        addGroup(sounds, new String[]{
                "superbwarfare:explosion_close"
        }, 1.0, 12.0);
    }

    private static void addGroup(Map<String, GeneralSoundsConfig.SoundEntry> sounds, String[] ids, double speed, double range) {
        for (String id : ids) {
            sounds.put(id, new GeneralSoundsConfig.SoundEntry(true, speed, range));
        }
    }

}
