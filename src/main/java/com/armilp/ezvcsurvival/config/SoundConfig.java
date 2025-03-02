package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.data.GunTypeModifiers;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = EZVCSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SoundConfig {

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOUND_GROUPS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_SOUND_REACTIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TACZ_GUN_TYPE_MODIFIERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> POINT_BLANK_GUN_TYPE_MODIFIERS;

    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE_MULTIPLIER;

    private static final Map<String, SoundGroupData> soundGroupDataMap = new HashMap<>();
    private static final Map<String, Map<String, Object>> mobReactionsMap = new HashMap<>();
    private static final Map<String, GunTypeModifiers> gunModifiersMap = new HashMap<>();
    private static final Map<String, GunTypeModifiers> pointBlankGunModifiersMap = new HashMap<>();

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Sound groups configuration",
                "Define reusable sound groups for mobs.",
                "Format: group_name=sound1,sound2,sound3[,speedMultiplier,rangeMultiplier]",
                "or: group_name=speed=VAL,range=VAL,sound1,sound2,...");
        builder.push("sound_groups");
        SOUND_GROUPS = builder.defineList("groups",
                () -> List.of(
                        "wood_sounds=block.wood.break,block.wood.hit,block.wood.place,1.0,1.0",
                        "animal_hurts=entity.cow.hurt,entity.pig.hurt"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );
        builder.pop();

        builder.comment("Mob sound reactions configuration",
                "Define sound reaction settings for each mob.",
                "Format: mob_id=speed=VALUE,range=VALUE,groups=group1,group2");
        builder.push("mob_sound_reactions");
        MOB_SOUND_REACTIONS = builder.defineList("reactions",
                () -> List.of(
                        "minecraft:zombie=speed=1.5,range=20,groups=wood_sounds",
                        "minecraft:cow=speed=1.8,range=16,groups=animal_hurts"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );
        builder.pop();

        builder.push("weather");
        builder.comment("Range multiplier applied when it is raining or thundering.",
                "Value between 0 and 1. A lower value will reduce the effective sound range.");
        THUNDER_RANGE_MULTIPLIER = builder.defineInRange("thunder_range_multiplier", 0.8, 0.0, 1.0);
        builder.pop();


        builder.comment("Gun type modifiers configuration (TACZ)",
                "Define multipliers for mob reaction speed and range by gun type.",
                "Format: gunType=speedMultiplier,rangeMultiplier");
        builder.push("tacz_gun_type_modifiers");
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

        builder.comment("PointBlank gun type modifiers configuration",
                "Define multipliers for pointblank reaction adjustments by gun type.",
                "Format: gunType=speedMultiplier,rangeMultiplier");
        builder.push("pointblank_gun_type_modifiers");
        POINT_BLANK_GUN_TYPE_MODIFIERS = builder.defineList("modifiers",
                () -> List.of(
                        "pistol=1.0,4.0",
                        "sniper=1.0,5.0",
                        "rifle=1.0,6.5",
                        "shotgun=1.0,4.8",
                        "smg=1.0,3.0",
                        "rpg=1.0,10.0",
                        "mg=1.0,8.0"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );
        builder.pop();

        SPEC = builder.build();
    }

    @SubscribeEvent
    public static void onModConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("Loading the EZVCSurvival configuration (initial load)...");
            loadConfigs();
        }
    }

    @SubscribeEvent
    public static void onModConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("Reloading the EZVCSurvival configuration...");
            loadConfigs();
        }
    }

    public static void loadConfigs() {
        loadSoundGroups();
        loadMobSoundReactions();
        loadGunTypeModifiers();
        loadPointBlankGunTypeModifiers();
    }

    private static void loadSoundGroups() {
        soundGroupDataMap.clear();
        List<? extends String> groups = SOUND_GROUPS.get();
        if (groups == null || groups.isEmpty()) {
            groups = List.of(
                    "wood_sounds=block.wood.break,block.wood.hit,block.wood.place,1.0,1.0",
                    "animal_hurts=entity.cow.hurt,entity.pig.hurt"
            );
        }
        for (String entry : groups) {
            String[] parts = entry.split("=", 2);
            if (parts.length < 2) {
                EZVCSurvival.LOGGER.warn("Invalid sound group entry: " + entry);
                continue;
            }
            String groupName = parts[0].trim();
            List<String> tokens = Arrays.stream(parts[1].split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            double speedMult = 1.0;
            double rangeMult = 1.0;
            if (!tokens.isEmpty() && tokens.get(0).toLowerCase().startsWith("speed=")) {
                try {
                    speedMult = Double.parseDouble(tokens.get(0).substring(6));
                    if (tokens.size() > 1 && tokens.get(1).toLowerCase().startsWith("range=")) {
                        rangeMult = Double.parseDouble(tokens.get(1).substring(6));
                        tokens = tokens.subList(2, tokens.size());
                    }
                } catch (NumberFormatException e) {
                    EZVCSurvival.LOGGER.warn("Error parsing multipliers in the entry: " + entry);
                }
            } else if (tokens.size() >= 3) {
                try {
                    speedMult = Double.parseDouble(tokens.get(tokens.size() - 2));
                    rangeMult = Double.parseDouble(tokens.get(tokens.size() - 1));
                    tokens = tokens.subList(0, tokens.size() - 2);
                } catch (NumberFormatException e) {
                    EZVCSurvival.LOGGER.warn("Error parsing multipliers in the entry: " + entry);
                }
            }
            if (tokens.isEmpty()) {
                EZVCSurvival.LOGGER.warn("No sounds defined for group: " + groupName);
                continue;
            }
            SoundGroupData data = new SoundGroupData(groupName, tokens, speedMult, rangeMult);
            soundGroupDataMap.put(groupName, data);
        }
    }

    private static void loadMobSoundReactions() {
        mobReactionsMap.clear();
        List<? extends String> reactions = MOB_SOUND_REACTIONS.get();
        if (reactions == null || reactions.isEmpty()) {
            reactions = List.of(
                    "minecraft:zombie=speed=1.5,range=20,groups=wood_sounds",
                    "minecraft:cow=speed=1.8,range=16,groups=animal_hurts"
            );
        }
        for (String entry : reactions) {
            String[] parts = entry.split("=", 2);
            if (parts.length < 2) {
                EZVCSurvival.LOGGER.warn("Invalid mob reaction entry: " + entry);
                continue;
            }
            String mobId = parts[0].trim();
            String params = parts[1].trim();
            Map<String, Object> map = new HashMap<>();
            int groupsIndex = params.indexOf("groups=");
            if (groupsIndex != -1) {
                String before = params.substring(0, groupsIndex);
                String after = params.substring(groupsIndex + 7);
                for (String token : before.split(",")) {
                    if (token.contains("=")) {
                        String[] kv = token.split("=", 2);
                        map.put(kv[0].trim(), tryParse(kv[1].trim()));
                    }
                }
                List<String> groupList = Arrays.stream(after.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                map.put("groups", groupList);
            } else {
                for (String token : params.split(",")) {
                    if (token.contains("=")) {
                        String[] kv = token.split("=", 2);
                        map.put(kv[0].trim(), tryParse(kv[1].trim()));
                    }
                }
            }
            if (map.isEmpty()) {
                EZVCSurvival.LOGGER.warn("No valid reaction defined for mob: " + mobId);
            } else {
                mobReactionsMap.put(mobId, map);
            }
        }
    }

    private static void loadGunTypeModifiers() {
        gunModifiersMap.clear();
        List<? extends String> modifiers = TACZ_GUN_TYPE_MODIFIERS.get();
        if (modifiers == null || modifiers.isEmpty()) {
            modifiers = List.of(
                    "pistol=1.0,3.8",
                    "sniper=1.0,5.0",
                    "rifle=1.0,6.5",
                    "shotgun=1.0,4.8",
                    "smg=1.0,3.0",
                    "rpg=1.0,10.0",
                    "mg=1.0,8.0"
            );
        }
        for (String entry : modifiers) {
            String[] parts = entry.split("=", 2);
            if (parts.length < 2) {
                EZVCSurvival.LOGGER.warn("Invalid gun modifier entry: " + entry);
                continue;
            }
            String[] types = parts[0].split(",");
            String[] values = parts[1].split(",");
            if (values.length != 2) {
                EZVCSurvival.LOGGER.warn("The gun modifier must have two values: " + entry);
                continue;
            }
            try {
                double speed = Double.parseDouble(values[0].trim());
                double range = Double.parseDouble(values[1].trim());
                for (String type : types) {
                    String t = type.trim().toLowerCase();
                    if (!t.isEmpty()) {
                        gunModifiersMap.put(t, new GunTypeModifiers(speed, range));
                        EZVCSurvival.LOGGER.info("Loaded modifier for gun: " + t + " -> speed: " + speed + ", range: " + range);
                    }
                }
            } catch (NumberFormatException e) {
                EZVCSurvival.LOGGER.warn("Error parsing gun modifier: " + entry, e);
            }
        }
    }

    private static void loadPointBlankGunTypeModifiers() {
        pointBlankGunModifiersMap.clear();
        List<? extends String> modifiers = POINT_BLANK_GUN_TYPE_MODIFIERS.get();
        if (modifiers == null || modifiers.isEmpty()) {
            modifiers = List.of(
                    "pistol=1.0,4.0",
                    "sniper=1.0,5.0",
                    "rifle=1.0,6.5",
                    "shotgun=1.0,4.8",
                    "smg=1.0,3.0",
                    "rpg=1.0,10.0",
                    "mg=1.0,8.0"
            );
        }
        for (String entry : modifiers) {
            String[] parts = entry.split("=", 2);
            if (parts.length < 2) {
                EZVCSurvival.LOGGER.warn("Invalid pointblank gun modifier entry: " + entry);
                continue;
            }
            String[] types = parts[0].split(",");
            String[] values = parts[1].split(",");
            if (values.length != 2) {
                EZVCSurvival.LOGGER.warn("The pointblank gun modifier must have two values: "  + entry);
                continue;
            }
            try {
                double speed = Double.parseDouble(values[0].trim());
                double range = Double.parseDouble(values[1].trim());
                for (String type : types) {
                    String t = type.trim().toLowerCase();
                    if (!t.isEmpty()) {
                        pointBlankGunModifiersMap.put(t, new GunTypeModifiers(speed, range));
                        EZVCSurvival.LOGGER.info("Loaded pointblank modifier for gun: " + t + " -> speed: " + speed + ", range: " + range);
                    }
                }
            } catch (NumberFormatException e) {
                EZVCSurvival.LOGGER.warn("Error parsing pointblank gun modifier: " + entry, e);
            }
        }
    }

    private static Object tryParse(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return s;
        }
    }

    public static List<SoundGroupData> getSoundGroupsForMob(String mobId) {
        List<SoundGroupData> list = new ArrayList<>();
        Map<String, Object> reaction = mobReactionsMap.get(mobId);
        if (reaction != null && reaction.containsKey("groups")) {
            @SuppressWarnings("unchecked")
            List<String> groups = (List<String>) reaction.get("groups");
            for (String group : groups) {
                SoundGroupData data = soundGroupDataMap.get(group);
                if (data != null) {
                    list.add(data);
                } else {
                    EZVCSurvival.LOGGER.warn("Group not found: " + group + " para el mob: " + mobId);
                }
            }
        }
        return list;
    }

    public static double getSpeedMultiplier(String gunType) {
        GunTypeModifiers mod = gunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.speedMultiplier : 1.0;
    }

    public static double getRangeMultiplier(String gunType) {
        GunTypeModifiers mod = gunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.rangeMultiplier : 1.0;
    }

    public static double getPointBlankSpeedMultiplier(String gunType) {
        GunTypeModifiers mod = pointBlankGunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.speedMultiplier : 1.0;
    }

    public static double getPointBlankRangeMultiplier(String gunType) {
        GunTypeModifiers mod = pointBlankGunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.rangeMultiplier : 1.0;
    }

    public static Map<String, Object> getMobSoundReaction(String mobId) {
        return mobReactionsMap.get(mobId);
    }
}
