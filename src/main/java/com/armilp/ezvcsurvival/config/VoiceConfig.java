package com.armilp.ezvcsurvival.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoiceConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_VOICE_CONFIGS;

    public static final ForgeConfigSpec.DoubleValue WHISPER_RANGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WHISPER_SPEED_MULTIPLIER;

    static {
        BUILDER.comment("FollowVoice Config").push("FollowVoice Config");

        MOB_VOICE_CONFIGS = BUILDER.comment(
                "List of mob configurations for FollowVoice.",
                "Format: 'mob_id=speed=<value>,range=<value>,threshold=<value>'",
                "Example: 'minecraft:zombie=speed=1.5,range=25,threshold=-10.0'",
                "The 'threshold' value determines how easily the mob can hear you.",
                "If it's lower (e.g., -10), it will struggle more to hear you, but if set to -120, the mob will hear you with minimal effort."
        ).defineList(
                "mob_configs",
                List.of(
                        "minecraft:zombie=speed=1.5,range=20,threshold=-40.0",
                        "minecraft:skeleton=speed=1.2,range=15,threshold=-35.0"
                ),
                obj -> obj instanceof String && ((String) obj).contains("=")
        );

        BUILDER.pop();

        BUILDER.comment("Whisper Config").push("Whisper Config");

        WHISPER_RANGE_MULTIPLIER = BUILDER.comment(
                "Multiplier for detection range when the player is whispering.",
                "This multiplies the range you have configured for your mobs.",
                "Example: range=20 x 0.5 = 10"
        ).defineInRange("whisper_range_multiplier", 0.5, 0.0, 1.0);

        WHISPER_SPEED_MULTIPLIER = BUILDER.comment(
                "Multiplier for mob speed when the player is whispering.",
                "This multiplies the speed you have configured for your mobs.",
                "Example: speed=1.2 x 0.8 = 0.96"
        ).defineInRange("whisper_speed_multiplier", 0.8, 0.0, 1.0);


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
