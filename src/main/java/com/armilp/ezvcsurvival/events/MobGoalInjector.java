package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class MobGoalInjector {

    private static final String TAG = "ezvcsurvival:goals_added";
    private static final Set<Mob> TRACKED_MOBS = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        Level level = event.getLevel();
        if (level.isClientSide) return;
        if (mob.isRemoved()) return;

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        String mobIdStr = id != null ? id.toString() : "unknown";

        try {
            removeOldGoals(mob);
            injectGoals(mob);

            CompoundTag data = mob.getPersistentData();
            data.putBoolean(TAG, true);
            TRACKED_MOBS.add(mob);

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Injected goals into: " + mobIdStr + " (UUID=" + mob.getUUID() + ")");
            }
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
            if (mob == null || mob.isRemoved()) {
                it.remove();
                continue;
            }
            refreshMob(mob);
        }
    }

    public static void refreshEntityId(String entityId) {
        for (Mob mob : new ArrayList<>(TRACKED_MOBS)) {
            if (mob == null || mob.isRemoved()) continue;
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
            if (id != null && id.toString().equals(entityId)) {
                refreshMob(mob);
            }
        }
    }

    private static void refreshMob(Mob mob) {
        if (mob == null || mob.isRemoved()) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        String mobIdStr = id != null ? id.toString() : "unknown";

        try {
            CompoundTag data = mob.getPersistentData();
            data.remove(TAG);

            removeOldGoals(mob);
            injectGoals(mob);

            data.putBoolean(TAG, true);

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Refreshed goals for: " + mobIdStr + " (UUID=" + mob.getUUID() + ")");
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error al refrescar goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void injectGoals(Mob mob) {
        if (mob == null || mob.isRemoved()) return;

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (id == null) return;
        String mobId = id.toString();

        // Monsters follow voice (non-animals)
        try {
            if (EntityVoiceConfig.isEnabled() && !(mob instanceof Animal)) {
                EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobId);
                if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                    mob.goalSelector.addGoal(0, new FollowVoiceGoal(mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000));
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject FollowVoiceGoal for " + mobId + ": " + e.getMessage());
            }
        }

        // Animals runaway voice
        try {
            if (EntityVoiceConfig.isEnabled() && mob instanceof Animal animal) {
                EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(mobId);
                if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                    animal.goalSelector.addGoal(4, new RunawayVoiceGoal(animal, cfg.speed, cfg.range, cfg.threshold));
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject RunawayVoiceGoal for " + mobId + ": " + e.getMessage());
            }
        }

        // General sound reactions
        try {
            if (GeneralSoundsConfig.isEnabled()) {
                GeneralSoundsConfig.Reaction r = GeneralSoundsConfig.getMobReactions().get(mobId);
                if (r != null && r.enabled && r.speed > 0 && r.range > 0) {
                    List<SoundGroupData> groups = SoundConfig.getEnabledSoundGroups();
                    if (!groups.isEmpty()) {
                        mob.goalSelector.addGoal(2, new ReactToGeneralSoundGoal(mob, r.speed, r.range, groups));
                    }
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject ReactToGeneralSoundGoal for " + mobId + ": " + e.getMessage());
            }
        }
    }

    private static void removeOldGoals(Mob mob) {
        try {
            mob.goalSelector.getAvailableGoals().removeIf(w -> {
                Class<?> goalClass = w.getGoal().getClass();
                return FollowVoiceGoal.class.isAssignableFrom(goalClass) ||
                        RunawayVoiceGoal.class.isAssignableFrom(goalClass) ||
                        ReactToGeneralSoundGoal.class.isAssignableFrom(goalClass);
            });
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error removing old goals: " + e.getMessage());
            }
        }
    }
}