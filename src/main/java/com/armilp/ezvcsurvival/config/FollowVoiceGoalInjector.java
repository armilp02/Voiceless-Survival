package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.FollowVoiceGoal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class FollowVoiceGoalInjector {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Verificar si la entidad es un Mob
        if (!(event.getEntity() instanceof Mob mob)) return;

        // Obtener el ResourceLocation del Mob
        ResourceLocation mobId = mob.getType().builtInRegistryHolder().key().location();

        // Cargar configuraciones
        Map<String, Map<String, Double>> configs = VoiceConfig.getMobVoiceConfigs();
        if (!configs.containsKey(mobId.toString())) return;

        // Obtener la configuración específica del mob
        Map<String, Double> mobConfig = configs.get(mobId.toString());
        double speed = mobConfig.getOrDefault("speed", 1.0);
        int range = mobConfig.getOrDefault("range", 16.0).intValue();
        double threshold = mobConfig.getOrDefault("threshold", -40.0);

        // Agregar FollowVoiceGoal al Mob
        mob.goalSelector.addGoal(1, new FollowVoiceGoal(mob, speed, range, threshold));
    }
}
