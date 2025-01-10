package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.FollowVoiceGoal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = EZVCSurvival.MOD_ID)
public class FollowVoiceGoalInjector {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        // Obtén la configuración del mob
        Map<String, String> configs = VoiceConfig.getMobVoiceConfigs();
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

        if (configs.containsKey(mobId.toString())) {
            String config = configs.get(mobId.toString());

            // Parsear velocidad y rango desde la configuración
            double speed = parseValue(config, "speed", 1.0);
            int range = (int) parseValue(config, "range", 16);

            // Agregar el objetivo
            mob.goalSelector.addGoal(1, new FollowVoiceGoal(mob, speed, range));
        }
    }

    private static double parseValue(String config, String key, double defaultValue) {
        try {
            String[] parts = config.split(",");
            for (String part : parts) {
                String[] keyValue = part.split("=");
                if (keyValue[0].equalsIgnoreCase(key)) {
                    return Double.parseDouble(keyValue[1]);
                }
            }
        } catch (Exception e) {
            EZVCSurvival.LOGGER.warn("Error parsing config value for key {}: {}", key, config);
        }
        return defaultValue;
    }
}
