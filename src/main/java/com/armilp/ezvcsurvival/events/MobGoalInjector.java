package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.*;

public class MobGoalInjector {

    private static final String TAG = "ezvcsurvival_goals_added";
    private static final Set<Mob> TRACKED_MOBS = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        Level level = event.getLevel();
        if (level.isClientSide) return;
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean(TAG)) {
            TRACKED_MOBS.add(mob);
            return;
        }
        try {
            injectGoals(mob);
            data.putBoolean(TAG, true);
            TRACKED_MOBS.add(mob);
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error al inyectar goals en evento: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    public static void refreshAll() {
        Iterator<Mob> it = TRACKED_MOBS.iterator();
        while (it.hasNext()) {
            Mob mob = it.next();
            if (mob.isRemoved()) {
                it.remove();
                continue;
            }
            refreshMob(mob);
        }
    }

    public static void refreshEntityId(String entityId) {
        for (Mob mob : TRACKED_MOBS) {
            if (mob.isRemoved()) continue;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (id != null && id.toString().equals(entityId)) {
                refreshMob(mob);
            }
        }
    }

    private static void refreshMob(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(TAG);
        removeOldGoals(mob);
        try {
            injectGoals(mob);
            data.putBoolean(TAG, true);
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error al refrescar goals: " + e.getMessage());
            }
        }
    }

    private static void injectGoals(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (id == null) return;
        String mobId = id.toString();

        if (EntityVoiceConfig.isEnabled() && !(mob instanceof Animal)) {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                mob.goalSelector.addGoal(0, new FollowVoiceGoal(mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000));
            }
        }

        if (EntityVoiceConfig.isEnabled() && mob instanceof Animal animal) {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                animal.goalSelector.addGoal(4, new RunawayVoiceGoal(animal, cfg.speed, cfg.range, cfg.threshold));
            }
        }

        if (GeneralSoundsConfig.isEnabled()) {
            GeneralSoundsConfig.Reaction r = GeneralSoundsConfig.getMobReactions().get(mobId);
            if (r != null && r.enabled && r.speed > 0 && r.range > 0) {
                List<SoundGroupData> groups = SoundConfig.getEnabledSoundGroups();
                if (!groups.isEmpty()) {
                    mob.goalSelector.addGoal(2, new ReactToGeneralSoundGoal(mob, r.speed, r.range, groups));
                }
            }
        }
    }

    private static void removeOldGoals(Mob mob) {
        mob.goalSelector.getAvailableGoals().removeIf(w ->
                w.getGoal().getClass().getName().startsWith("com.armilp.ezvcsurvival.goals"));
    }
}