package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Map;

public class FollowVoiceGoalInjector {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        ResourceLocation mobId = mob.getType().builtInRegistryHolder().key().location();

        Map<String, Map<String, Double>> configs = VoiceConfig.getMobVoiceConfigs();
        if (!configs.containsKey(mobId.toString())) return;

        Map<String, Double> mobConfig = configs.get(mobId.toString());
        double speed = mobConfig.getOrDefault("speed", 1.0);
        int range = mobConfig.getOrDefault("range", 16.0).intValue();
        double threshold = mobConfig.getOrDefault("threshold", -40.0);

        mob.goalSelector.addGoal(1, new FollowVoiceGoal(mob, speed, range, threshold));
    }
}
