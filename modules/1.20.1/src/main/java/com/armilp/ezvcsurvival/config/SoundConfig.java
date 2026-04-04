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
    public static final ForgeConfigSpec.BooleanValue ENABLE_SILENCER_MODIFIERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TACZ_SILENCER_MODIFIERS;
    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue DEBUG;

    private static final List<SoundGroupData> priorityGroups = new ArrayList<>();
    private static final Map<String, GunTypeModifiers> gunModifiersMap = new HashMap<>();
    private static final Map<String, GunTypeModifiers> silencerModifiersMap = new HashMap<>();
    private static final Set<String> silencedGunIds = new HashSet<>();
    private static final List<SoundGroupData> customSoundGroups = new ArrayList<>();

    public static final ForgeConfigSpec.BooleanValue ENABLE_GUNFIRE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GENERAL_SOUNDS;

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("GunFire Generation",
                        "If true, automatically adds new entities to gunfire.json when detected",
                        "If false, only uses entities already in the file (allows manual control)")
                .push("gunfire_generation");
        ENABLE_GUNFIRE = builder.define("enable_generation", true);
        builder.pop();

        builder.comment("GeneralSounds Generation",
                        "If true, automatically adds new entities to generalsounds.json when detected",
                        "If false, only uses entities and sounds already in the file (allows manual control)")
                .push("generalsounds_generation");
        ENABLE_GENERAL_SOUNDS = builder.define("enable_generation", true);
        builder.pop();

        builder.comment("Debug Mode")
                .push("debugging");
        DEBUG = builder.define("debug", false);
        builder.pop();

        builder.push("weather");
        THUNDER_RANGE_MULTIPLIER = builder.defineInRange("thunder_range_multiplier", 0.8, 0.0, 1.0);
        builder.pop();

        builder.push("tacz_gun_modifiers");
        builder.comment(
                "Gun type modifiers in format: type=speed,range",
                "Speed: multiplier for mob reaction speed",
                "Range: multiplier for detection range",
                "Example: 'pistol=1.0,3.8' means normal speed, 3.8x range"
        );
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

        builder.push("tacz_silencer_modifiers");
        builder.comment(
                "If false, silenced guns are completely ignored by mobs (original behavior)"
        );
        ENABLE_SILENCER_MODIFIERS = builder.define("enabled", true);
        builder.comment(
                "Silencer modifiers per gun type in format: type=speed,range"
        );
        TACZ_SILENCER_MODIFIERS = builder.defineList("modifiers",
                () -> List.of(
                        "pistol=0.5,0.4",
                        "sniper=0.5,0.35",
                        "rifle=0.5,0.4",
                        "shotgun=0.5,0.45",
                        "smg=0.5,0.4",
                        "rpg=0.5,0.5",
                        "mg=0.5,0.45"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );
        builder.pop();

        SPEC = builder.build();

        loadDefaultGunTypeModifiers();
        loadDefaultSilencerModifiers();
    }

    @SubscribeEvent
    public static void onModConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("[SoundConfig] Loading configuration...");
            loadConfigs();
        }
    }

    @SubscribeEvent
    public static void onModConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            EZVCSurvival.LOGGER.info("[SoundConfig] Reloading configuration...");
            loadConfigs();
        }
    }

    public static void loadConfigs() {
        loadGunTypeModifiers();
        loadSilencedGuns();
        loadSilencerModifiers();

        try {
            GeneralSoundsConfig.init();
            GunfireConfig.init();
            mergeGeneralSoundsFromJson();
            refreshPriorityGroups();
        } catch (Exception e) {
            EZVCSurvival.LOGGER.warn("[SoundConfig] Error loading JSON configs: {}", e.getMessage());
        }
    }

    private static void loadDefaultGunTypeModifiers() {
        if (isDebugEnabled()) {
            EZVCSurvival.LOGGER.info("[SoundConfig] Loading DEFAULT gun type modifiers...");
        }

        gunModifiersMap.put("pistol",  new GunTypeModifiers(1.0, 3.8));
        gunModifiersMap.put("sniper",  new GunTypeModifiers(1.0, 8.0));
        gunModifiersMap.put("rifle",   new GunTypeModifiers(1.0, 6.5));
        gunModifiersMap.put("shotgun", new GunTypeModifiers(1.0, 4.8));
        gunModifiersMap.put("smg",     new GunTypeModifiers(1.0, 3.0));
        gunModifiersMap.put("rpg",     new GunTypeModifiers(1.0, 10.0));
        gunModifiersMap.put("mg",      new GunTypeModifiers(1.0, 4.0));

        if (isDebugEnabled()) {
            EZVCSurvival.LOGGER.info("[SoundConfig] Loaded {} default gun modifiers", gunModifiersMap.size());
            gunModifiersMap.forEach((key, value) ->
                    EZVCSurvival.LOGGER.info("[SoundConfig]   '{}' -> speed={}x, range={}x",
                            key, value.speedMultiplier(), value.rangeMultiplier())
            );
        }
    }

    private static void loadGunTypeModifiers() {
        gunModifiersMap.clear();
        List<? extends String> modifiers = TACZ_GUN_TYPE_MODIFIERS.get();

        if (isDebugEnabled()) {
            EZVCSurvival.LOGGER.info("[SoundConfig] Loading {} gun modifiers from config...", modifiers.size());
        }

        for (String entry : modifiers) {
            String[] parts = entry.split("=", 2);
            if (parts.length < 2) {
                EZVCSurvival.LOGGER.warn("[SoundConfig] Invalid entry (no '='): {}", entry);
                continue;
            }

            String[] types = parts[0].split(",");
            String[] values = parts[1].split(",");

            if (values.length != 2) {
                EZVCSurvival.LOGGER.warn("[SoundConfig] Invalid values (need 2): {}", entry);
                continue;
            }

            try {
                double speed = Double.parseDouble(values[0].trim());
                double range = Double.parseDouble(values[1].trim());

                for (String type : types) {
                    String t = type.trim().toLowerCase();
                    if (!t.isEmpty()) {
                        gunModifiersMap.put(t, new GunTypeModifiers(speed, range));
                        if (isDebugEnabled()) {
                            EZVCSurvival.LOGGER.info("[SoundConfig] Loaded: '{}' -> speed={}x, range={}x",
                                    t, speed, range);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                EZVCSurvival.LOGGER.warn("[SoundConfig] Invalid numbers in: {}", entry);
            }
        }

        if (isDebugEnabled()) {
            EZVCSurvival.LOGGER.info("[SoundConfig] Config load complete: {} types", gunModifiersMap.size());
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

    private static void loadDefaultSilencerModifiers() {
        silencerModifiersMap.put("pistol",  new GunTypeModifiers(1.0, 0.4));
        silencerModifiersMap.put("sniper",  new GunTypeModifiers(1.0, 0.35));
        silencerModifiersMap.put("rifle",   new GunTypeModifiers(1.0, 0.4));
        silencerModifiersMap.put("shotgun", new GunTypeModifiers(1.0, 0.45));
        silencerModifiersMap.put("smg",     new GunTypeModifiers(1.0, 0.4));
        silencerModifiersMap.put("rpg",     new GunTypeModifiers(1.0, 0.5));
        silencerModifiersMap.put("mg",      new GunTypeModifiers(1.0, 0.45));
    }

    private static void loadSilencerModifiers() {
        silencerModifiersMap.clear();
        List<? extends String> modifiers = TACZ_SILENCER_MODIFIERS.get();

        for (String entry : modifiers) {
            String[] parts = entry.split("=", 2);
            if (parts.length < 2) {
                EZVCSurvival.LOGGER.warn("[SoundConfig] Invalid silencer modifier entry: {}", entry);
                continue;
            }
            String[] values = parts[1].split(",");
            if (values.length != 2) {
                EZVCSurvival.LOGGER.warn("[SoundConfig] Invalid silencer modifier values: {}", entry);
                continue;
            }
            try {
                double speed = Double.parseDouble(values[0].trim());
                double range = Double.parseDouble(values[1].trim());
                silencerModifiersMap.put(parts[0].trim().toLowerCase(), new GunTypeModifiers(speed, range));
                if (isDebugEnabled()) {
                    EZVCSurvival.LOGGER.info("[SoundConfig] Silencer modifier '{}' -> speed={}x, range={}x",
                            parts[0].trim(), speed, range);
                }
            } catch (NumberFormatException e) {
                EZVCSurvival.LOGGER.warn("[SoundConfig] Invalid numbers in silencer modifier: {}", entry);
            }
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
        GeneralSoundsConfig.processPrioritySounds(priorityGroups);
    }

    public static double getSpeedMultiplier(String gunType) {
        if (gunType == null || gunType.isEmpty()) return 1.0;
        GunTypeModifiers mod = gunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.speedMultiplier() : 1.0;
    }

    public static double getRangeMultiplier(String gunType) {
        if (gunType == null || gunType.isEmpty()) return 1.0;
        GunTypeModifiers mod = gunModifiersMap.get(gunType.toLowerCase());
        return mod != null ? mod.rangeMultiplier() : 1.0;
    }

    public static boolean isSilencedGun(ResourceLocation id) {
        return id != null && silencedGunIds.contains(id.toString().toLowerCase());
    }

    public static boolean isSilencerModifiersEnabled() {
        try {
            return ENABLE_SILENCER_MODIFIERS.get();
        } catch (Exception e) {
            return false;
        }
    }

    public static GunTypeModifiers getSilencerModifiers(String gunType) {
        if (gunType != null) {
            GunTypeModifiers mod = silencerModifiersMap.get(gunType.toLowerCase());
            if (mod != null) return mod;
            GunTypeModifiers def = silencerModifiersMap.get("default");
            if (def != null) return def;
        }
        return new GunTypeModifiers(1.0, 1.0);
    }

    public static List<SoundGroupData> getEnabledSoundGroups() {
        return Collections.unmodifiableList(customSoundGroups);
    }

    public static List<SoundGroupData> getPriorityGroups() {
        return Collections.unmodifiableList(priorityGroups);
    }

    public static boolean isDebugEnabled() {
        try {
            return DEBUG.get();
        } catch (Exception e) {
            return false;
        }
    }
}