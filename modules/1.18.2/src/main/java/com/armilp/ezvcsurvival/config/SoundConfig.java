package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.data.GunTypeModifiers;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.*;

@Mod.EventBusSubscriber(modid = EZVCSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SoundConfig {

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TACZ_GUN_TYPE_MODIFIERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SILENCED_GUN_IDS;

    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE_MULTIPLIER;

    private static final List<SoundGroupData> priorityGroups = new ArrayList<>();
    private static final Map<String, Map<String, Object>> generalReactionsMap = new HashMap<>();
    private static final Map<String, Map<String, Object>> gunfireReactionsMap = new HashMap<>();
    private static final Map<String, GunTypeModifiers> gunModifiersMap = new HashMap<>();
    private static final Set<String> silencedGunIds = new HashSet<>();

    private static final List<SoundGroupData> customSoundGroups = new ArrayList<>();

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("weather");
        THUNDER_RANGE_MULTIPLIER = builder.defineInRange("thunder_range_multiplier", 0.8, 0.0, 1.0);
        builder.pop();

        builder.push("tacz_gun_modifiers");
        TACZ_GUN_TYPE_MODIFIERS = builder.defineList("modifiers",
                () -> List.of(
                        "pistol=1.0,3.8",
                        "sniper=1.0,8.0",
                        "rifle=1.0,6.5",
                        "shotgun=1.0,4.8",
                        "smg=1.0,3.0",
                        "rpg=1.0,10.0",
                        "mg=1.0,4.0"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );
        builder.pop();

        builder.push("tacz_silenced_guns");
        SILENCED_GUN_IDS = builder.defineList("ids",
                () -> List.of(
                        "daffas_arsenal:hk45_sup",
                        "daffas_arsenal:apacoba9_sup"
                ),
                obj -> {
                    if (!(obj instanceof String)) return false;
                    String s = ((String) obj).trim();
                    return ResourceLocation.isValidResourceLocation(s);
                }
        );
        builder.pop();

        SPEC = builder.build();
    }

    @SubscribeEvent
    public static void onModConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("Loading EZVCSurvival configuration...");
            loadConfigs();
        }
    }

    @SubscribeEvent
    public static void onModConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("Reloading EZVCSurvival configuration...");
            loadConfigs();
        }
    }

    public static void loadConfigs() {
        loadGunTypeModifiers();
        loadSilencedGuns();

        try {
            GeneralSoundsConfig.init();
            GunfireConfig.init();
            mergeGeneralSoundsFromJson();
            mergeGunfireFromJson();
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
        Map<String, GeneralSoundsConfig.Reaction> mobs = GeneralSoundsConfig.getMobReactions();
        if (mobs != null) {
            for (Map.Entry<String, GeneralSoundsConfig.Reaction> e : mobs.entrySet()) {
                GeneralSoundsConfig.Reaction r = e.getValue();
                Map<String, Object> map = new HashMap<>();
                map.put("speed", r.speed);
                map.put("range", r.range);
                generalReactionsMap.put(e.getKey(), map);
            }
        }

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
        GeneralSoundsConfig.processPrioritySounds(priorityGroups);
    }

    private static void mergeGunfireFromJson() {
        Map<String, GunfireConfig.Reaction> mobs = GunfireConfig.getMobReactions();
        if (mobs != null) {
            for (Map.Entry<String, GunfireConfig.Reaction> e : mobs.entrySet()) {
                GunfireConfig.Reaction r = e.getValue();
                if (r == null) continue;
                Map<String, Object> map = new HashMap<>();
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

    private static void loadGunTypeModifiers() {
        gunModifiersMap.clear();
        List<? extends String> modifiers = TACZ_GUN_TYPE_MODIFIERS.get();
        for (String entry : modifiers) {
            String[] parts = entry.split("=", 2);
            if (parts.length < 2) continue;
            String[] types = parts[0].split(",");
            String[] values = parts[1].split(",");
            if (values.length != 2) continue;
            try {
                double speed = Double.parseDouble(values[0].trim());
                double range = Double.parseDouble(values[1].trim());
                for (String type : types) {
                    String t = type.trim().toLowerCase();
                    if (!t.isEmpty()) {
                        gunModifiersMap.put(t, new GunTypeModifiers(speed, range));
                    }
                }
            } catch (NumberFormatException e) {
                EZVCSurvival.LOGGER.warn("Error parsing gun modifier: {}", entry);
            }
        }
    }

    private static void loadSilencedGuns() {
        silencedGunIds.clear();
        List<? extends String> list = SILENCED_GUN_IDS.get();
        if (list != null) {
            for (String id : list) {
                silencedGunIds.add(id.trim().toLowerCase());
            }
        }
    }

    public static double getSpeedMultiplier(String gunType) {
        GunTypeModifiers mod = gunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.speedMultiplier() : 1.0;
    }

    public static double getRangeMultiplier(String gunType) {
        GunTypeModifiers mod = gunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.rangeMultiplier() : 1.0;
    }

    public static boolean isSilencedGun(ResourceLocation id) {
        return id != null && silencedGunIds.contains(id.toString().toLowerCase());
    }

    public static List<SoundGroupData> getPriorityGroups() {
        return Collections.unmodifiableList(priorityGroups);
    }
}