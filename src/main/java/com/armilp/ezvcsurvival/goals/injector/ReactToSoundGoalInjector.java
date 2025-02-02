package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class ReactToSoundGoalInjector {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        Map<String, Map<String, Object>> configs = VoiceConfig.getSoundReactionConfigs();
        ResourceLocation mobId = Registry.ENTITY_TYPE.getKey(mob.getType());

        if (configs.containsKey(mobId.toString())) {
            Map<String, Object> config = configs.get(mobId.toString());

            double speed = config.get("speed") instanceof Number
                    ? ((Number) config.get("speed")).doubleValue()
                    : 1.0;
            double rangeDouble = config.get("range") instanceof Number
                    ? ((Number) config.get("range")).doubleValue()
                    : 16.0;
            int range = (int) rangeDouble;

            @SuppressWarnings("unchecked")
            List<String> soundTypes = config.get("sound_types") instanceof List<?>
                    ? (List<String>) config.get("sound_types")
                    : List.of();

            if (!soundTypes.isEmpty()) {
                mob.goalSelector.addGoal(3, new ReactToSoundGoal(mob, speed, range, soundTypes));
            }
        }
    }
}
