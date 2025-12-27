package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.util.*;

@Mod.EventBusSubscriber(modid = EZVCSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SoundConfig {

    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE_MULTIPLIER;

    private static final List<SoundGroupData> priorityGroups = new ArrayList<SoundGroupData>();
    private static final Map<String, Map<String, Object>> generalReactionsMap = new HashMap<String, Map<String, Object>>();
    private static final Map<String, Map<String, Object>> gunfireReactionsMap = new HashMap<String, Map<String, Object>>();

    private static final List<SoundGroupData> customSoundGroups = new ArrayList<SoundGroupData>();

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("weather");
        THUNDER_RANGE_MULTIPLIER = builder.defineInRange("thunder_range_multiplier", 0.8, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }

    @SubscribeEvent
    public static void onModConfigLoading(ModConfig.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("Loading EZVCSurvival configuration...");
            loadConfigs();
        }
    }

    @SubscribeEvent
    public static void onModConfigReloading(ModConfig.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("Reloading EZVCSurvival configuration...");
            loadConfigs();
        }
    }

    public static void loadConfigs() {
        try {
            GeneralSoundsConfig.init();
            GunfireConfig.init();
            mergeGeneralSoundsFromJson();
            mergeGunfireFromJson();
            refreshPriorityGroups();
        } catch (Exception e) {
            EZVCSurvival.LOGGER.warning("Error loading JSON sound configs: {}");
        }
    }

    private static void refreshPriorityGroups() {
        priorityGroups.clear();
        GeneralSoundsConfig.processPrioritySounds(priorityGroups);
    }

    private static void mergeGeneralSoundsFromJson() {
        Map<String, GeneralSoundsConfig.Reaction> mobs = GeneralSoundsConfig.getMobReactions();
        if (mobs != null) {
            for (Map.Entry<String, GeneralSoundsConfig.Reaction> e : mobs.entrySet()) {
                GeneralSoundsConfig.Reaction r = e.getValue();
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("speed", r.speed);
                map.put("range", r.range);
                generalReactionsMap.put(e.getKey(), map);
            }
        }

        Iterator<SoundGroupData> iterator = customSoundGroups.iterator();
        while (iterator.hasNext()) {
            SoundGroupData g = iterator.next();
            if (g.groupName.startsWith("auto_sound_")) {
                iterator.remove();
            }
        }

        Map<String, GeneralSoundsConfig.SoundEntry> sounds = GeneralSoundsConfig.getSounds();
        if (sounds != null) {
            for (Map.Entry<String, GeneralSoundsConfig.SoundEntry> e : sounds.entrySet()) {
                GeneralSoundsConfig.SoundEntry se = e.getValue();
                customSoundGroups.add(new SoundGroupData(
                        "auto_sound_" + e.getKey().replace(':', '_').replace('.', '_'),
                        Collections.singletonList(e.getKey()),
                        se.speed_multiplier,
                        se.range_multiplier
                ));
            }
        }
        GeneralSoundsConfig.processPrioritySounds(priorityGroups);
    }

    private static void mergeGunfireFromJson() {
        Map<String, GunfireConfig.Reaction> mobs = GunfireConfig.getMobReactions();
        if (mobs != null) {
            for (Map.Entry<String, GunfireConfig.Reaction> e : mobs.entrySet()) {
                GunfireConfig.Reaction r = e.getValue();
                if (r == null) continue;
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("speed", r.speed);
                map.put("range", r.range);
                map.put("enabled", r.enabled);
                gunfireReactionsMap.put(e.getKey(), map);
            }
        }
    }

    public static List<SoundGroupData> getEnabledSoundGroups() {
        return Collections.unmodifiableList(customSoundGroups);
    }

    public static double getSpeedMultiplier(String gunType) {
        return 1.0;
    }

    public static double getRangeMultiplier(String gunType) {
        return 1.0;
    }

    public static List<SoundGroupData> getPriorityGroups() {
        return Collections.unmodifiableList(priorityGroups);
    }
}