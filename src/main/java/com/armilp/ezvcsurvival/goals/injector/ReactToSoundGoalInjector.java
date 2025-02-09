package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class ReactToSoundGoalInjector {

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        ResourceLocation mobIdRL = Registry.ENTITY_TYPE.getKey(mob.getType());
        String mobId = mobIdRL.toString();

        Map<String, Object> config = SoundConfig.getMobSoundReaction(mobId);
        if (config != null) {
            double speed = config.get("speed") instanceof Number
                    ? ((Number) config.get("speed")).doubleValue()
                    : 1.0;
            double rangeDouble = config.get("range") instanceof Number
                    ? ((Number) config.get("range")).doubleValue()
                    : 16.0;
            int range = (int) rangeDouble;
            List<?> groups = (List<?>) config.get("groups");
            if (groups != null && !groups.isEmpty()) {
                var soundGroups = SoundConfig.getSoundGroupsForMob(mobId);
                if (!soundGroups.isEmpty()) {
                    mob.goalSelector.addGoal(3, new ReactToSoundGoal(mob, speed, range, soundGroups));
                }
            }
        }
    }
}
