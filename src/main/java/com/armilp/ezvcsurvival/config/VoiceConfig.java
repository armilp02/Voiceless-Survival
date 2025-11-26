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

    public static final ForgeConfigSpec.DoubleValue DEATH_ANGELS_THRESHOLD;

    public static final ForgeConfigSpec.DoubleValue QUIET_PLACE_OVERMAN_THRESHOLD;

    public static final ForgeConfigSpec.BooleanValue MOB_SPEED_BOOST_ENABLED;

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

        BUILDER.comment("Death Angels Mod Config",
                        "Defines the threshold detection (how hard the player must speak)",
                        "You need this mod for this parameter: https://www.curseforge.com/minecraft/mc-mods/death-angels")
                .push("death_angels_config");
        DEATH_ANGELS_THRESHOLD = BUILDER.defineInRange("death_angels_threshold", -20.0, -127.0, 0.0);
        BUILDER.pop();

        BUILDER.comment("OverMan's Quiet place Mod Config",
                        "Defines the threshold detection (how hard the player must speak)",
                        "You need this mod for this parameter: https://www.curseforge.com/minecraft/mc-mods/overmans-quiet-place")
                .push("quiet_place_overman_config");
        QUIET_PLACE_OVERMAN_THRESHOLD = BUILDER.defineInRange("quiet_place_overman_threshold", -30.0, -127.0, 0.0);
        BUILDER.pop();

        BUILDER.comment("Debugging Config")
                .push("debugging");
        DEBUG = BUILDER.define("debug", false);
        BUILDER.pop();

        CONFIG = BUILDER.build();
    }
}