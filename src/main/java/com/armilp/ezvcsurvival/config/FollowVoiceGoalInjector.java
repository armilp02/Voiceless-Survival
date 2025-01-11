package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.FollowVoiceGoal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class FollowVoiceGoalInjector {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        Map<String, Map<String, Double>> configs = VoiceConfig.getMobVoiceConfigs();
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

        if (configs.containsKey(mobId.toString())) {
            Map<String, Double> config = configs.get(mobId.toString());
            double speed = config.getOrDefault("speed", 1.0);
            double range = config.getOrDefault("range", 16.0);
            double threshold = config.getOrDefault("threshold", -40.0);

            mob.goalSelector.addGoal(1, new FollowVoiceGoal(mob, speed, (int) range, threshold));
        }
    }
}
