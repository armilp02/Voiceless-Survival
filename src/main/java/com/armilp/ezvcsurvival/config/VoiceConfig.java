package com.armilp.ezvcsurvival.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoiceConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_VOICE_CONFIGS;

    static {
        BUILDER.push("FollowVoiceGoal Config");

        MOB_VOICE_CONFIGS = BUILDER.comment(
                "List of mob configurations for FollowVoiceGoal.",
                "Format: 'mob_id=speed=<value>,range=<value>,threshold=<value>'",
                "Example: 'minecraft:zombie=speed=1.5,range=25,threshold=-10.0'"
        ).defineList(
                "mob_configs",
                List.of(
                        "minecraft:zombie=speed=1.5,range=25,threshold=-10.0",
                        "minecraft:skeleton=speed=1.2,range=25,threshold=-20.0"
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
}
