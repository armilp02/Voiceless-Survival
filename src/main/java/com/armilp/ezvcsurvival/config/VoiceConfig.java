package com.armilp.ezvcsurvival.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoiceConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_VOICE_CONFIGS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ANIMAL_VOICE_CONFIGS;

    public static final ForgeConfigSpec.DoubleValue WHISPER_RANGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WHISPER_SPEED_MULTIPLIER;

    public static final ForgeConfigSpec.DoubleValue THUNDER_RANGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SNEAKING_RANGE_MULTIPLIER;

    // Nueva opción para el umbral del efecto "death_angels"
    public static final ForgeConfigSpec.DoubleValue DEATH_ANGELS_THRESHOLD;

    static {

        BUILDER.comment("FollowVoice Config",
                        "Defines how mobs react to player voices, including their movement speed, detection range, and reaction threshold.")
                .push("mob_voice_configs");
        MOB_VOICE_CONFIGS = BUILDER.defineList(
                "mob_configs",
                List.of(
                        "minecraft:zombie=speed=1.5,range=20,threshold=-40.0",
                        "minecraft:skeleton=speed=1.2,range=15,threshold=-35.0"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );
        BUILDER.pop();


        BUILDER.comment("RunawayVoiceGoal Config",
                        "Defines how animals react to player voices, including their fleeing speed, detection range, and reaction threshold.")
                .push("animal_voice_configs");
        ANIMAL_VOICE_CONFIGS = BUILDER.defineList(
                "animal_configs",
                List.of(
                        "minecraft:cow=speed=1.5,range=15,threshold=-45.0",
                        "minecraft:pig=speed=1.2,range=5,threshold=-45.0"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );
        BUILDER.pop();


        BUILDER.comment("Whisper Config",
                        "Multipliers that affect the detection range and movement speed when the player is whispering.")
                .push("whisper_configs");
        WHISPER_RANGE_MULTIPLIER = BUILDER.defineInRange("whisper_range_multiplier", 0.5, 0.0, 1.0);
        WHISPER_SPEED_MULTIPLIER = BUILDER.defineInRange("whisper_speed_multiplier", 0.8, 0.0, 1.0);
        BUILDER.pop();


        BUILDER.comment("Misc Config",
                        "Multipliers that affect the detection range of voices in specific situations.")
                .push("misc_config");
        THUNDER_RANGE_MULTIPLIER = BUILDER.defineInRange("thunder_range_multiplier", 0.5, 0.0, 1.0);
        SNEAKING_RANGE_MULTIPLIER = BUILDER.defineInRange("sneaking_range_multiplier", 0.5, 0.0, 1.0);
        BUILDER.pop();

        BUILDER.comment("Death Angels Mod Config",
                        "Defines the threshold detection (how hard the player must speak)",
                        "You need this mod for this parameter: https://www.curseforge.com/minecraft/mc-mods/death-angels")
                .push("death_angels_config");
        DEATH_ANGELS_THRESHOLD = BUILDER.defineInRange("death_angels_threshold", -30.0, -127.0, 0.0);
        BUILDER.pop();

        CONFIG = BUILDER.build();
    }

    public static Map<String, Map<String, Double>> getMobVoiceConfigs() {
        Map<String, Map<String, Double>> parsedConfigs = new HashMap<>();
        for (String config : MOB_VOICE_CONFIGS.get()) {
            String[] parts = config.split("=", 2);
            if (parts.length == 2) {
                String mobId = parts[0];
                String[] attributes = parts[1].split(",");
                Map<String, Double> mobConfig = new HashMap<>();
                for (String attribute : attributes) {
                    String[] keyValue = attribute.split("=");
                    if (keyValue.length == 2) {
                        try {
                            mobConfig.put(keyValue[0].trim(), Double.parseDouble(keyValue[1].trim()));
                        } catch (NumberFormatException e) {
                            System.err.println("[VoiceConfig] Invalid number format in: " + attribute);
                        }
                    }
                }
                parsedConfigs.put(mobId, mobConfig);
            }
        }
        return parsedConfigs;
    }

    public static Map<String, Map<String, Double>> getAnimalVoiceConfigs() {
        Map<String, Map<String, Double>> parsedConfigs = new HashMap<>();
        for (String config : ANIMAL_VOICE_CONFIGS.get()) {
            String[] parts = config.split("=", 2);
            if (parts.length == 2) {
                String mobId = parts[0];
                String[] attributes = parts[1].split(",");
                Map<String, Double> mobConfig = new HashMap<>();
                for (String attribute : attributes) {
                    String[] keyValue = attribute.split("=");
                    if (keyValue.length == 2) {
                        try {
                            mobConfig.put(keyValue[0].trim(), Double.parseDouble(keyValue[1].trim()));
                        } catch (NumberFormatException e) {
                            System.err.println("[VoiceConfig] Invalid number format in: " + attribute);
                        }
                    }
                }
                parsedConfigs.put(mobId, mobConfig);
            }
        }
        return parsedConfigs;
    }
}
