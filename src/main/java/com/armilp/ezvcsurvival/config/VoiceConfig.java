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


    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SOUND_REACTION_CONFIGS;

    static {
        BUILDER.push("FollowVoice Config");

        MOB_VOICE_CONFIGS = BUILDER.comment(
                "List of mob configurations for FollowVoice.",
                "Format: 'mob_id=speed=<value>,range=<value>,threshold=<value>'",
                "Example: 'minecraft:zombie=speed=1.5,range=25,threshold=-10.0'"
        ).defineList(
                "mob_configs",
                List.of(
                        "minecraft:zombie=speed=1.5,range=20,threshold=-40.0",
                        "minecraft:skeleton=speed=1.2,range=15,threshold=-35.0"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );

        ANIMAL_VOICE_CONFIGS = BUILDER.comment(
                "List of mob configurations for RunawayVoiceGoal.",
                "Format: 'animal_id=speed=<value>,range=<value>,threshold=<value>'",
                "Example: 'minecraft:cow=speed=1.0,range=15,threshold=-25.0'"
        ).defineList(
                "animal_configs",
                List.of(
                        "minecraft:cow=speed=1.5,range=15,threshold=-45.0",
                        "minecraft:pig=speed=1.2,range=5,threshold=-45.0"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );

        BUILDER.pop();

        BUILDER.push("Whisper Config");

        WHISPER_RANGE_MULTIPLIER = BUILDER.comment(
                "Multiplier for detection range when the player is whispering.",
                "Example: range=20 x 0.5 = 10"
        ).defineInRange("whisper_range_multiplier", 0.5, 0.0, 1.0);

        WHISPER_SPEED_MULTIPLIER = BUILDER.comment(
                "Multiplier for mob speed when the player is whispering.",
                "Example: speed=1.2 x 0.8 = 0.96"
        ).defineInRange("whisper_speed_multiplier", 0.8, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.push("Misc Config");

        THUNDER_RANGE_MULTIPLIER = BUILDER.comment(
                "Multiplier for detection range when it is raining or during a thunderstorm (reduces the range)."
        ).defineInRange("thunder_range_multiplier", 0.5, 0.0, 1.0);

        SNEAKING_RANGE_MULTIPLIER = BUILDER.comment(
                "Multiplier for detection range when the player is sneaking/crouching (reduces the range)."
        ).defineInRange("sneaking_range_multiplier", 0.5, 0.0, 1.0);

        SOUND_REACTION_CONFIGS = BUILDER.comment(
                "List of sound reaction configurations for mobs.",
                "Format: 'mob_id=speed=<value>,range=<value>,sound_types=<type1,type2,...>'",
                "Example: 'minecraft:zombie=speed=1.5,range=20,sound_types=block.wood.break,block.metal.hit,modded:custom.sound'",
                "How to know the sound types? Use the command /playsound <sound> <source> <player>"
        ).defineList(
                "sound_reaction_configs",
                List.of(
                        "minecraft:zombie=speed=1.5,range=20,sound_types=block.wood.break,block.metal.hit",
                        "minecraft:cow=speed=1.8,range=16,sound_types=entity.cow.death,entity.cow.hurt"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );


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

    public static Map<String, Map<String, Object>> getSoundReactionConfigs() {
        Map<String, Map<String, Object>> parsedConfigs = new HashMap<>();
        for (String config : SOUND_REACTION_CONFIGS.get()) {
            String[] parts = config.split("=", 2);
            if (parts.length == 2) {
                String mobId = parts[0];
                String remaining = parts[1];
                String[] tokens = remaining.split(",");
                Map<String, Object> mobConfig = new HashMap<>();
                String currentKey = null;
                for (String token : tokens) {
                    if (token.contains("=")) {
                        String[] keyValue = token.split("=", 2);
                        currentKey = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        if ("sound_types".equals(currentKey)) {
                            List<String> sounds = new java.util.ArrayList<>();
                            sounds.add(value);
                            mobConfig.put(currentKey, sounds);
                        } else {
                            try {
                                mobConfig.put(currentKey, Double.parseDouble(value));
                            } catch (NumberFormatException e) {
                                System.err.println("[VoiceConfig] Invalid number format in: " + token);
                            }
                        }
                    } else {
                        if ("sound_types".equals(currentKey)) {
                            @SuppressWarnings("unchecked")
                            List<String> sounds = (List<String>) mobConfig.get("sound_types");
                            sounds.add(token.trim());
                        } else {
                            System.err.println("[VoiceConfig] Unexpected token without '=': " + token);
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