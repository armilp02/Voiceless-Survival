package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

@Config(modid = EZVCSurvival.MOD_ID, name = "ezvcsurvival/sounds")
@Config.LangKey("ezvcsurvival.config.sounds")
public class SoundConfig {

    @Config.Comment("Weather effects on sound detection")
    @Config.Name("Thunder Range Multiplier")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double THUNDER_RANGE_MULTIPLIER = 0.8;

    private static final List<SoundGroupData> priorityGroups = new ArrayList<SoundGroupData>();
    private static final Map<String, Map<String, Object>> generalReactionsMap = new HashMap<String, Map<String, Object>>();
    private static final Map<String, Map<String, Object>> gunfireReactionsMap = new HashMap<String, Map<String, Object>>();
    private static final List<SoundGroupData> customSoundGroups = new ArrayList<SoundGroupData>();

    public static void init(java.io.File configDir) {
        // La config se carga automáticamente
    }

    public static void loadConfigs() {
        try {
            GeneralSoundsConfig.init();
            mergeGeneralSoundsFromJson();
            refreshPriorityGroups();
        } catch (Exception e) {
            EZVCSurvival.LOGGER.warn("Error loading JSON sound configs: " + e.getMessage());
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

    @Mod.EventBusSubscriber(modid = EZVCSurvival.MOD_ID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(EZVCSurvival.MOD_ID)) {
                ConfigManager.sync(EZVCSurvival.MOD_ID, Config.Type.INSTANCE);
                loadConfigs();
            }
        }
    }
}