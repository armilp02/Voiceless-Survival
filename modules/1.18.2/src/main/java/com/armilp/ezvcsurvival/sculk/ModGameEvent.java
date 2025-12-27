package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;

import java.lang.reflect.Field;

public class ModGameEvent {

    public static GameEvent VOICE_TALK;

    public static void register() {
        EZVCSurvival.LOGGER.info("=== REGISTERING VOICE_TALK GAMEEVENT ===");

        VOICE_TALK = Registry.register(
                Registry.GAME_EVENT,
                new ResourceLocation(EZVCSurvival.MOD_ID, "voice_talk"),
                new GameEvent("voice_talk", 16)
        );

        EZVCSurvival.LOGGER.info("VOICE_TALK registered: " + VOICE_TALK);

        try {
            // Acceder al campo estático VIBRATION_FREQUENCY_FOR_EVENT mediante reflexión
            Field frequencyField = VibrationListener.class.getDeclaredField("VIBRATION_FREQUENCY_FOR_EVENT");
            frequencyField.setAccessible(true);
            Object frequencyMapObj = frequencyField.get(null);

            EZVCSurvival.LOGGER.info("Frequency map type: " + frequencyMapObj.getClass().getName());

            if (frequencyMapObj instanceof Object2IntOpenHashMap) {
                @SuppressWarnings("unchecked")
                Object2IntOpenHashMap<GameEvent> map = (Object2IntOpenHashMap<GameEvent>) frequencyMapObj;
                int frequency = VoiceConfig.SCULK_SENSOR_FREQUENCY.get();
                map.put(VOICE_TALK, frequency);

                int storedFrequency = map.getInt(VOICE_TALK);
                EZVCSurvival.LOGGER.info("Successfully registered VOICE_TALK with frequency: " + frequency);
                EZVCSurvival.LOGGER.info("Verification - stored frequency: " + storedFrequency);
            } else {
                EZVCSurvival.LOGGER.error("VIBRATION_FREQUENCY_FOR_EVENT is not an Object2IntOpenHashMap!");
            }
        } catch (NoSuchFieldException e) {
            EZVCSurvival.LOGGER.error("Could not find VIBRATION_FREQUENCY_FOR_EVENT field", e);
        } catch (IllegalAccessException e) {
            EZVCSurvival.LOGGER.error("Could not access VIBRATION_FREQUENCY_FOR_EVENT field", e);
        } catch (Exception e) {
            EZVCSurvival.LOGGER.error("Failed to register VOICE_TALK frequency", e);
        }

        EZVCSurvival.LOGGER.info("=== VOICE_TALK REGISTRATION COMPLETE ===");
    }
}