package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

import java.util.function.ToIntFunction;

public class ModGameEvent {

    public static GameEvent VOICE_TALK;

    public static void register() {
        VOICE_TALK = Registry.register(
                BuiltInRegistries.GAME_EVENT,
                ResourceLocation.fromNamespaceAndPath(EZVCSurvival.MOD_ID, "voice_talk"),
                new GameEvent(16)
        );

        try {
            ToIntFunction<ResourceKey<GameEvent>> frequencyMap = VibrationSystem.VIBRATION_FREQUENCY_FOR_EVENT;

            if (frequencyMap instanceof @SuppressWarnings("unchecked")Object2IntOpenHashMap<ResourceKey<GameEvent>> map) {

                ResourceKey<GameEvent> voiceTalkKey = BuiltInRegistries.GAME_EVENT.getResourceKey(VOICE_TALK).orElse(null);

                if (voiceTalkKey != null) {
                    int frequency = VoiceConfig.SCULK_SENSOR_FREQUENCY.get();
                    map.put(voiceTalkKey, frequency);

                    int storedFrequency = map.getInt(voiceTalkKey);
                    EZVCSurvival.LOGGER.info("Registered VOICE_TALK with frequency: " + storedFrequency);
                } else {
                    EZVCSurvival.LOGGER.error("Failed to get ResourceKey for VOICE_TALK");
                }
            } else {
                EZVCSurvival.LOGGER.error("VIBRATION_FREQUENCY_FOR_EVENT is not an Object2IntOpenHashMap!");
            }
        } catch (Exception e) {
            EZVCSurvival.LOGGER.error("Failed to register VOICE_TALK frequency", e);
        }
    }
}