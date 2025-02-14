package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Map;

@EventBusSubscriber(modid = "ezvcsurvival")
public class RunawayVoiceGoalInjector {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Animal animal)) return;

        ResourceLocation animalId = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());

        Map<String, Map<String, Double>> configs = VoiceConfig.getAnimalVoiceConfigs();
        if (!configs.containsKey(animalId.toString())) return;

        Map<String, Double> mobConfig = configs.get(animalId.toString());
        double speed = mobConfig.getOrDefault("speed", 1.0);
        int range = mobConfig.getOrDefault("range", 16.0).intValue();
        double threshold = mobConfig.getOrDefault("threshold", -40.0);

        animal.goalSelector.addGoal(1, new RunawayVoiceGoal(animal, speed, range, threshold));
    }
}