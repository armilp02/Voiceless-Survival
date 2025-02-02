package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class FollowVoiceGoalInjector {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof MobEntity)) {
            return;
        }

        MobEntity mob = (MobEntity) event.getEntity();

        Map<String, Map<String, Double>> configs = VoiceConfig.getMobVoiceConfigs();
        ResourceLocation mobId = Registry.ENTITY_TYPE.getKey(mob.getType());

        if (configs.containsKey(mobId.toString())) {
            Map<String, Double> config = configs.get(mobId.toString());
            double speed = config.getOrDefault("speed", 1.0);
            double range = config.getOrDefault("range", 16.0);
            double threshold = config.getOrDefault("threshold", -40.0);

            mob.goalSelector.addGoal(1, new FollowVoiceGoal(mob, speed, (int) range, threshold, 10000));
        }
    }
}
