package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = EZVCSurvival.MOD_ID, name = "ezvcsurvival/voices")
@Config.LangKey("ezvcsurvival.config.voices")
public class VoiceConfig {

    @Config.Comment({
            "Whisper Config",
            "Multipliers that affect the detection range and movement speed when the player is whispering."
    })
    @Config.Name("Whisper Settings")
    public static WhisperSettings WHISPER = new WhisperSettings();

    @Config.Comment({
            "Mob Speed Boost Config",
            "Enable or disable the mob speed boost when targeting speaking players"
    })
    @Config.Name("Mob Speed Boost")
    public static boolean MOB_SPEED_BOOST_ENABLED = true;

    @Config.Comment({
            "Misc Config",
            "Multipliers that affect the detection range of voices in specific situations."
    })
    @Config.Name("Miscellaneous Settings")
    public static MiscSettings MISC = new MiscSettings();

    @Config.Comment({
            "Armor Effects Config",
            "Define multipliers for mob detection range and speed when a player wears specific armor items.",
            "Format: item_id=speedMultiplier,rangeMultiplier",
            "Example: minecraft:diamond_helmet=0.5,0.5"
    })
    @Config.Name("Armor Effects")
    public static String[] ARMOR_EFFECTS = new String[]{
            "minecraft:diamond_helmet=0.9,0.6",
            "minecraft:diamond_chestplate=1.0,0.7"
    };

    @Config.Comment("Debugging Config")
    @Config.Name("Debug Mode")
    public static boolean DEBUG = false;

    public static class WhisperSettings {
        @Config.Comment("Range multiplier when whispering")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        @Config.Name("Whisper Range Multiplier")
        public double whisper_range_multiplier = 0.5;

        @Config.Comment("Speed multiplier when whispering")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        @Config.Name("Whisper Speed Multiplier")
        public double whisper_speed_multiplier = 0.8;
    }

    public static class MiscSettings {
        @Config.Comment("Range multiplier during thunder/rain")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        @Config.Name("Thunder Range Multiplier")
        public double thunder_range_multiplier = 0.65;

        @Config.Comment("Range multiplier when sneaking")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        @Config.Name("Sneaking Range Multiplier")
        public double sneaking_range_multiplier = 0.5;
    }

    // Accesos convenientes para compatibilidad
    public static double WHISPER_RANGE_MULTIPLIER = WHISPER.whisper_range_multiplier;
    public static double WHISPER_SPEED_MULTIPLIER = WHISPER.whisper_speed_multiplier;
    public static double THUNDER_RANGE_MULTIPLIER = MISC.thunder_range_multiplier;
    public static double SNEAKING_RANGE_MULTIPLIER = MISC.sneaking_range_multiplier;

    public static void init(java.io.File configDir) {
        // La config se carga automáticamente
    }

    @Mod.EventBusSubscriber(modid = EZVCSurvival.MOD_ID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(EZVCSurvival.MOD_ID)) {
                ConfigManager.sync(EZVCSurvival.MOD_ID, Config.Type.INSTANCE);

                // Actualizar las referencias estáticas
                WHISPER_RANGE_MULTIPLIER = WHISPER.whisper_range_multiplier;
                WHISPER_SPEED_MULTIPLIER = WHISPER.whisper_speed_multiplier;
                THUNDER_RANGE_MULTIPLIER = MISC.thunder_range_multiplier;
                SNEAKING_RANGE_MULTIPLIER = MISC.sneaking_range_multiplier;
            }
        }
    }
}