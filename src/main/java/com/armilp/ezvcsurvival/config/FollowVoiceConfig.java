package com.armilp.ezvcsurvival.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FollowVoiceConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_VOICE_CONFIGS;

    static {
        BUILDER.push("FollowVoiceGoal Config");

        // Configuración dinámica basada en listas
        MOB_VOICE_CONFIGS = BUILDER.comment(
                "List of mob configurations for FollowVoiceGoal.",
                "Format: 'mob_id=speed=<value>,range=<value>'",
                "Example: 'minecraft:zombie=speed=1.0,range=16'"
        ).defineList(
                "mob_configs",
                List.of(
                        "minecraft:zombie=speed=1.0,range=16",
                        "minecraft:skeleton=speed=1.2,range=12"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );

        BUILDER.pop();
        CONFIG = BUILDER.build();
    }

    public static Map<String, String> getMobVoiceConfigs() {
        Map<String, String> parsedConfigs = new HashMap<>();

        // Parsear las configuraciones dinámicas desde la lista
        for (String config : MOB_VOICE_CONFIGS.get()) {
            String[] parts = config.split("=", 2);
            if (parts.length == 2) {
                String mobId = parts[0];
                String mobConfig = parts[1];
                parsedConfigs.put(mobId, mobConfig);
            }
        }

        return parsedConfigs;
    }
}
