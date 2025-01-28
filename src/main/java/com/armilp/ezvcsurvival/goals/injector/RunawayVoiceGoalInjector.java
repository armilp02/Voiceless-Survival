package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class RunawayVoiceGoalInjector {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof AnimalEntity)) {
            return; // Verifica si la entidad es un AnimalEntity
        }

        AnimalEntity animal = (AnimalEntity) event.getEntity(); // Cast explícito

        Map<String, Map<String, Double>> configs = VoiceConfig.getAnimalVoiceConfigs();
        ResourceLocation animalId = Registry.ENTITY_TYPE.getKey(animal.getType());

        if (configs.containsKey(animalId.toString())) {
            Map<String, Double> config = configs.get(animalId.toString());
            double speed = config.getOrDefault("speed", 1.0);
            double range = config.getOrDefault("range", 16.0);
            double threshold = config.getOrDefault("threshold", -40.0);

            animal.goalSelector.addGoal(1, new RunawayVoiceGoal(animal, speed, (int) range, threshold));
        }
    }
}
