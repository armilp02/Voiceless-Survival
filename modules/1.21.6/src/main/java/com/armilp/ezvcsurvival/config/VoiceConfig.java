package com.armilp.ezvcsurvival.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class VoiceConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ARMOR_EFFECTS;

    public static final ForgeConfigSpec.DoubleValue WHISPER_RANGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WHISPER_SPEED_MULTIPLIER;

    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SNEAKING_RANGE_MULTIPLIER;

    public static final ForgeConfigSpec.BooleanValue MOB_SPEED_BOOST_ENABLED;

    // Sculk Sensor Config
    public static final ForgeConfigSpec.BooleanValue SCULK_SENSOR_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SCULK_SENSOR_THRESHOLD;
    public static final ForgeConfigSpec.IntValue SCULK_SENSOR_RANGE;
    public static final ForgeConfigSpec.IntValue SCULK_SENSOR_FREQUENCY;

    public static final ForgeConfigSpec.BooleanValue DEBUG;

    static {
        BUILDER.comment("Whisper Config",
                        "Multipliers that affect the detection range and movement speed when the player is whispering.")
                .push("whisper_configs");
        WHISPER_RANGE_MULTIPLIER = BUILDER.defineInRange("whisper_range_multiplier", 0.5, 0.0, 1.0);
        WHISPER_SPEED_MULTIPLIER = BUILDER.defineInRange("whisper_speed_multiplier", 0.8, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Mob Speed Boost Config",
                        "Enable or disable the mob speed boost when targeting speaking players")
                .push("mob_speed_boost");
        MOB_SPEED_BOOST_ENABLED = BUILDER.define("mob_speed_boost_enabled", true);
        BUILDER.pop();

        BUILDER.comment("Misc Config",
                        "Multipliers that affect the detection range of voices in specific situations.")
                .push("misc_config");
        THUNDER_RANGE_MULTIPLIER = BUILDER.defineInRange("thunder_range_multiplier", 0.65, 0.0, 1.0);
        SNEAKING_RANGE_MULTIPLIER = BUILDER.defineInRange("sneaking_range_multiplier", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Sculk Sensor Voice Detection Config",
                        "Configure sculk sensors to activate when players speak via voice chat",
                        "Enable: Toggle sculk sensor activation from voice",
                        "Threshold: Minimum audio level (dB) required to trigger sculk sensors (-127 to 0)",
                        "Range: Maximum distance (blocks) from player to activate sculk sensors",
                        "Frequency: Redstone signal strength output (1-15)")
                .push("sculk_sensor_config");
        SCULK_SENSOR_ENABLED = BUILDER.define("sculk_sensor_enabled", true);
        SCULK_SENSOR_THRESHOLD = BUILDER.defineInRange("sculk_sensor_threshold", -45.0, -127.0, 0.0);
        SCULK_SENSOR_RANGE = BUILDER.defineInRange("sculk_sensor_range", 16, 1, 100);
        SCULK_SENSOR_FREQUENCY = BUILDER.defineInRange("sculk_sensor_frequency", 8, 1, 15);
        BUILDER.pop();

        BUILDER.comment("Armor Effects Config",
                "Define multipliers for mob detection range and speed when a player wears specific armor items.",
                "Format: item_id=speedMultiplier,rangeMultiplier",
                "Example: minecraft:diamond_helmet=0.5,0.5");
        BUILDER.push("armor_effects");
        ARMOR_EFFECTS = BUILDER.defineList("effects",
                () -> List.of("minecraft:diamond_helmet=0.9,0.6", "minecraft:diamond_chestplate=1.0,0.7"),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );
        BUILDER.pop();

        BUILDER.comment("Debugging Config")
                .push("debugging");
        DEBUG = BUILDER.define("debug", false);
        BUILDER.pop();

        CONFIG = BUILDER.build();
    }
}