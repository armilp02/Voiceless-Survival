package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SoundConfig {

    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE_MULTIPLIER;

    private static final List<SoundGroupData> priorityGroups = new ArrayList<>();
    private static final List<SoundGroupData> customSoundGroups = new ArrayList<>();

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("weather");
        THUNDER_RANGE_MULTIPLIER = builder.defineInRange("thunder_range_multiplier", 0.8, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }

    public static void registerConfigListeners(net.minecraftforge.eventbus.api.bus.BusGroup modBusGroup) {
        ModConfigEvent.Loading.getBus(modBusGroup).addListener(SoundConfig::onModConfigLoading);
        ModConfigEvent.Reloading.getBus(modBusGroup).addListener(SoundConfig::onModConfigReloading);
    }

    private static void onModConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("Loading EZVCSurvival configuration...");
            loadConfigs();
        }
    }

    private static void onModConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("Reloading EZVCSurvival configuration...");
            loadConfigs();
        }
    }

    public static void loadConfigs() {
        try {
            GeneralSoundsConfig.init();
            mergeGeneralSoundsFromJson();
            refreshPriorityGroups();
        } catch (Exception e) {
            EZVCSurvival.LOGGER.warn("Error loading JSON sound configs: {}", e.getMessage());
        }
    }

    private static void refreshPriorityGroups() {
        priorityGroups.clear();
        GeneralSoundsConfig.processPrioritySounds(priorityGroups);
    }

    private static void mergeGeneralSoundsFromJson() {
        customSoundGroups.removeIf(g -> g.groupName().startsWith("auto_sound_"));

        Map<String, GeneralSoundsConfig.SoundEntry> sounds = GeneralSoundsConfig.getSounds();
        if (sounds != null) {
            for (Map.Entry<String, GeneralSoundsConfig.SoundEntry> e : sounds.entrySet()) {
                GeneralSoundsConfig.SoundEntry se = e.getValue();
                customSoundGroups.add(new SoundGroupData(
                        "auto_sound_" + e.getKey().replace(':', '_').replace('.', '_'),
                        List.of(e.getKey()),
                        se.speed_multiplier,
                        se.range_multiplier
                ));
            }
        }
    }

    public static List<SoundGroupData> getEnabledSoundGroups() {
        return Collections.unmodifiableList(customSoundGroups);
    }
}