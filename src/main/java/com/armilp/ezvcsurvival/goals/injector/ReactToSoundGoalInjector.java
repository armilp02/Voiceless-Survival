package com.armilp.ezvcsurvival.goals.injector;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import net.minecraft.core.registries.BuiltInRegistries;
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

        ResourceLocation mobIdRL = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        String mobId = mobIdRL.toString();

        // Cargar configuración general de reacciones a sonidos
        Map<String, Object> soundConfig = SoundConfig.getMobSoundReaction(mobId);
        if (soundConfig != null) {
            double speed = soundConfig.get("speed") instanceof Number
                    ? ((Number) soundConfig.get("speed")).doubleValue()
                    : 1.0;
            double rangeDouble = soundConfig.get("range") instanceof Number
                    ? ((Number) soundConfig.get("range")).doubleValue()
                    : 16.0;
            int range = (int) rangeDouble;

            // Obtener los grupos de sonido para este mob
            List<?> groups = (List<?>) soundConfig.get("groups");
            if (groups != null && !groups.isEmpty()) {
                var soundGroups = SoundConfig.getSoundGroupsForMob(mobId);
                if (!soundGroups.isEmpty()) {
                    mob.goalSelector.addGoal(2, new ReactToSoundGoal(mob, speed, range, soundGroups));
                }
            }
        }

        // Para las reacciones de disparos de armas (gunfire), también revisamos la configuración
        List<SoundGroupData> gunfireGroups = SoundConfig.getSoundGroupsForGunFireMob(mobId);
        if (!gunfireGroups.isEmpty()) {
            double speed = 1.0;
            int range = 20;

            // Actualizar con los valores de la configuración si están presentes
            if (soundConfig != null) {
                if (soundConfig.containsKey("speed")) {
                    speed = ((Number) soundConfig.get("speed")).doubleValue();
                }
                if (soundConfig.containsKey("range")) {
                    range = ((Number) soundConfig.get("range")).intValue();
                }
            }

            // Añadir el goal ReactToSoundGoal para reaccionar a los disparos
            mob.goalSelector.addGoal(2, new ReactToSoundGoal(mob, speed, range, gunfireGroups));
        }
    }
}
