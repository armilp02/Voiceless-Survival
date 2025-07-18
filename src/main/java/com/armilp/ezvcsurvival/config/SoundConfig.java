package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.data.GunTypeModifiers;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import net.minecraft.resources.ResourceLocation;
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
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SILENCED_GUN_IDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PRIORITY_SOUNDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GUN_FIRE_MOBS;

    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE_MULTIPLIER;

    private static final List<SoundGroupData> priorityGroups = new ArrayList<>();

    private static final Map<String, SoundGroupData> soundGroupDataMap = new HashMap<>();
    private static final Map<String, Map<String, Object>> mobReactionsMap = new HashMap<>();
    private static final Map<String, GunTypeModifiers> gunModifiersMap = new HashMap<>();
    private static final Map<String, GunTypeModifiers> pointBlankGunModifiersMap = new HashMap<>();
    private static final Set<String> silencedGunIds = new HashSet<>();

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
                        "wood_sounds=block.wood.break,block.wood.place,block.wood.hit,1.0,1.0",
                        "stone_sounds=block.stone.break,block.stone.place,block.stone.hit,1.0,1.0",
                        "metal_sounds=block.metal.break,block.metal.place,block.metal.hit,1.0,1.0",
                        "glass_sounds=block.glass.break,block.glass.place,1.0,1.0",
                        "gravel_sounds=block.gravel.break,block.gravel.place,1.0,1.0",
                        "sand_sounds=block.sand.break,block.sand.place,1.0,1.0",

                        "animal_hurts=entity.cow.hurt,entity.pig.hurt,entity.sheep.hurt,entity.chicken.hurt,entity.rabbit.hurt",
                        "hostile_hurts=entity.zombie.hurt,entity.skeleton.hurt,entity.creeper.hurt,entity.spider.hurt",

                        "fire_sounds=block.fire.ambient,block.fire.extinguish,item.firecharge.use",
                        "explosive_sounds=entity.generic.explode,entity.creeper.primed,entity.tnt.primed",

                        "anvil_sounds=block.anvil.break,block.anvil.use,block.anvil.place",
                        "bell_sounds=block.bell.use,block.bell.resonate"
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
                        "minecraft:zombie=speed=1.5,range=20,groups=wood_sounds,stone_sounds,metal_sounds,glass_sounds,gravel_sounds,sand_sounds,animal_hurts,hostile_hurts,fire_sounds,explosive_sounds,anvil_sounds,bell_sounds",
                        "minecraft:cow=speed=1.8,range=16,groups=wood_sounds,stone_sounds,metal_sounds,glass_sounds,gravel_sounds,sand_sounds,animal_hurts,hostile_hurts,fire_sounds,explosive_sounds,anvil_sounds,bell_sounds"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );
        builder.pop();

        builder.push("priority_group");
        builder.comment("Define priority sounds with per-sound speed and range",
                "Format: namespace:path=speed,range");
        PRIORITY_SOUNDS = builder.defineList("sounds",
                () -> List.of("minecraft:entity.generic.explode=1.0,12.0"),
                obj -> obj instanceof String && ((String) obj).contains("=")
                        && ResourceLocation.isValidResourceLocation(((String) obj).split("=", 2)[0])
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

        builder.push("gun_fire_mobs");
        GUN_FIRE_MOBS = builder.defineList("mobs",
                () -> List.of(
                        "minecraft:zombie",
                        "minecraft:skeleton"
                ),
                obj -> obj instanceof String && !((String) obj).isEmpty()
        );
        builder.pop();

        builder.comment("Silenced Guns configuration",
                "List of GUNS IDs that should always be treated as silenced.",
                "Format: namespace:id",
                "This is only for the guns are silenced by default (Only Addons from TACZ!)");
        builder.push("silenced_guns");
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
        loadSilencedGuns();
    }

    private static void loadSoundGroups() {
        soundGroupDataMap.clear();
        priorityGroups.clear();

        // parse priority sounds
        for (String entry : PRIORITY_SOUNDS.get()) {
            String[] parts = entry.split("=", 2);
            String soundId = parts[0].trim();
            String[] vals = parts[1].split(",");
            double speed = 1.0, range = 1.0;
            try {
                speed = Double.parseDouble(vals[0].trim());
                range = Double.parseDouble(vals[1].trim());
            } catch (NumberFormatException ignored) {}
            SoundGroupData data = new SoundGroupData(
                    "priority_" + soundId.replace(':', '_'),
                    List.of(soundId),
                    speed,
                    range
            );
            priorityGroups.add(data);
            soundGroupDataMap.put(data.groupName, data);
        }
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
                EZVCSurvival.LOGGER.warn("The pointblank gun modifier must have two values: " + entry);
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

    private static void loadSilencedGuns() {
        silencedGunIds.clear();
        List<? extends String> list = SILENCED_GUN_IDS.get();
        if (list != null) {
            for (String id : list) {
                silencedGunIds.add(id.trim().toLowerCase());
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

        list.addAll(priorityGroups);

        Map<String, Object> reaction = mobReactionsMap.get(mobId);
        if (reaction != null && reaction.containsKey("groups")) {
            @SuppressWarnings("unchecked")
            List<String> groups = (List<String>) reaction.get("groups");
            for (String group : groups) {
                SoundGroupData data = soundGroupDataMap.get(group);
                if (data != null) list.add(data);
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
    public static boolean isSilencedGun(ResourceLocation id) {
        return id != null && silencedGunIds.contains(id.toString().toLowerCase());
    }
    public static List<SoundGroupData> getPriorityGroups() {
        return Collections.unmodifiableList(priorityGroups);
    }

    public static List<SoundGroupData> getSoundGroupsForGunFireMob(String mobId) {
        List<SoundGroupData> list = new ArrayList<>();

        for (String entry : GUN_FIRE_MOBS.get()) {
            String[] parts = entry.split("=", 2);
            String entryMobId = parts[0].trim();

            if (entryMobId.equals(mobId)) {
                double speed = 1.0; // valor por defecto
                int range = 10;     // valor por defecto

                if (parts.length == 2) {
                    String params = parts[1].trim();
                    String[] paramPairs = params.split(",");
                    for (String param : paramPairs) {
                        String[] kv = param.split("=");
                        if (kv.length == 2) {
                            String key = kv[0].trim();
                            String value = kv[1].trim();
                            try {
                                if ("speed".equalsIgnoreCase(key)) {
                                    speed = Double.parseDouble(value);
                                } else if ("range".equalsIgnoreCase(key)) {
                                    range = Integer.parseInt(value);
                                }
                            } catch (NumberFormatException e) {
                                EZVCSurvival.LOGGER.warn("Error parsing gunfire parameters for " + mobId + ": " + param);
                            }
                        }
                    }
                }

                SoundGroupData gunfireGroup = new SoundGroupData(
                        "gunfire_sounds",
                        List.of("minecraft:entity.generic.explode"),
                        speed, range
                );
                list.add(gunfireGroup);
            }
        }

        return list;
    }
}
