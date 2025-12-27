
package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

import java.util.function.ToIntFunction;

public class ModGameEvent {

    public static GameEvent VOICE_TALK;

    public static void register() {

        VOICE_TALK = Registry.register(
                BuiltInRegistries.GAME_EVENT,
                new ResourceLocation(EZVCSurvival.MOD_ID, "voice_talk"),
                new GameEvent("voice_talk", 16)
        );

        try {
            ToIntFunction<GameEvent> frequencyMap = VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT;

            if (frequencyMap instanceof Object2IntOpenHashMap) {
                @SuppressWarnings("unchecked")
                Object2IntOpenHashMap<GameEvent> map = (Object2IntOpenHashMap<GameEvent>) frequencyMap;
                int frequency = VoiceConfig.SCULK_SENSOR_FREQUENCY.get();
                map.put(VOICE_TALK, frequency);
            } else {
                EZVCSurvival.LOGGER.error("VIBRATION_FREQUENCY_FOR_EVENT is not an Object2IntOpenHashMap!");
            }
        } catch (Exception e) {
            EZVCSurvival.LOGGER.error("Failed to register VOICE_TALK frequency", e);
        }

    }
}