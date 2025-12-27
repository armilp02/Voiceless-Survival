package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.GunfireConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.goals.ReactToGunfireGoal;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import com.armilp.ezvcsurvival.mixins.GoalSelectorAccessor;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobGoalInjector {

    private static final String TAG = "ezvcsurvival:goals_added";
    private static final Set<MobEntity> TRACKED_MOBS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<UUID, InjectedGoals> INJECTED_GOALS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof MobEntity)) {
            return;
        }

        MobEntity mob = (MobEntity) event.getEntity();
        World level = event.getWorld();

        if (level.isClientSide) {
            return;
        }

        if (mob.removed) {
            return;
        }

        try {
            if (!hasActiveGoals(mob)) {
                injectGoals(mob);

                CompoundNBT data = mob.getPersistentData();
                data.putBoolean(TAG, true);
                TRACKED_MOBS.add(mob);

                if (VoiceConfig.DEBUG.get()) {
                    ResourceLocation id = ForgeRegistries.ENTITIES.getKey(mob.getType());
                    System.out.println("[EZVCSurvival] Injected goals into: " + (id != null ? id : "unknown") + " (UUID=" + mob.getUUID() + ")");
                }
            } else {
                TRACKED_MOBS.add(mob);

                if (VoiceConfig.DEBUG.get()) {
                    ResourceLocation id = ForgeRegistries.ENTITIES.getKey(mob.getType());
                    System.out.println("[EZVCSurvival] Goals already present for: " + (id != null ? id : "unknown") + " (UUID=" + mob.getUUID() + ")");
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error injecting goals on join: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static boolean hasActiveGoals(MobEntity mob) {
        InjectedGoals injected = INJECTED_GOALS.get(mob.getUUID());
        if (injected != null && injected.hasAnyGoal()) {
            return verifyGoalsInSelector(mob, injected);
        }
        return false;
    }

    private static boolean verifyGoalsInSelector(MobEntity mob, InjectedGoals injected) {
        Set<PrioritizedGoal> availableGoals = ((GoalSelectorAccessor) mob.goalSelector).getAvailableGoals();

        boolean hasFollowGoal = injected.followGoal == null ||
                availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.followGoal);
        boolean hasRunawayGoal = injected.runawayGoal == null ||
                availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.runawayGoal);
        boolean hasGeneralSoundGoal = injected.generalSoundGoal == null ||
                availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.generalSoundGoal);
        boolean hasGunfireGoal = injected.gunfireGoal == null ||
                availableGoals.stream().anyMatch(wg -> wg.getGoal() == injected.gunfireGoal);

        return hasFollowGoal && hasRunawayGoal && hasGeneralSoundGoal && hasGunfireGoal;
    }

    public static void refreshAll() {
        Iterator<MobEntity> it = TRACKED_MOBS.iterator();
        while (it.hasNext()) {
            MobEntity mob = it.next();
            if (mob == null || mob.removed) {
                it.remove();
                continue;
            }
            refreshMob(mob);
        }
    }

    public static void refreshEntityId(String entityId) {
        for (MobEntity mob : new ArrayList<>(TRACKED_MOBS)) {
            if (mob == null || mob.removed) {
                continue;
            }
            ResourceLocation id = ForgeRegistries.ENTITIES.getKey(mob.getType());
            if (id != null && id.toString().equals(entityId)) {
                refreshMob(mob);
            }
        }
    }

    private static void refreshMob(MobEntity mob) {
        if (mob == null || mob.removed) {
            return;
        }
        try {
            cleanupOldGoals(mob);
            injectGoals(mob);

            if (VoiceConfig.DEBUG.get()) {
                ResourceLocation id = ForgeRegistries.ENTITIES.getKey(mob.getType());
                System.out.println("[EZVCSurvival] Refreshed goals for: " + (id != null ? id : "unknown") + " (UUID=" + mob.getUUID() + ")");
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error refreshing goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void injectGoals(MobEntity mob) {
        if (mob == null || mob.removed) {
            return;
        }

        ResourceLocation id = ForgeRegistries.ENTITIES.getKey(mob.getType());
        if (id == null) {
            return;
        }
        String mobId = id.toString();

        InjectedGoals injectedGoals = new InjectedGoals();
        boolean isAnimal = mob instanceof AnimalEntity;

        if (EntityVoiceConfig.isEnabled()) {
            if (!isAnimal) {
                injectedGoals.followGoal = injectFollowVoiceGoal(mob, mobId);
            } else {
                injectedGoals.runawayGoal = injectRunawayVoiceGoal((AnimalEntity) mob, mobId);
            }
        }

        if (GeneralSoundsConfig.isEnabled()) {
            injectedGoals.generalSoundGoal = injectGeneralSoundGoal(mob, mobId);
        }

        if (GunfireConfig.isEnabled()) {
            injectedGoals.gunfireGoal = injectGunfireGoal(mob, mobId);
        }

        INJECTED_GOALS.put(mob.getUUID(), injectedGoals);
    }

    private static Goal injectFollowVoiceGoal(MobEntity mob, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                Goal goal = new FollowVoiceGoal(mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000);
                mob.goalSelector.addGoal(2, goal);
                return goal;
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject FollowVoiceGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Goal injectRunawayVoiceGoal(AnimalEntity animal, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                Goal goal = new RunawayVoiceGoal(animal, cfg.speed, (int) cfg.range, cfg.threshold);
                animal.goalSelector.addGoal(4, goal);
                return goal;
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject RunawayVoiceGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Goal injectGeneralSoundGoal(MobEntity mob, String mobId) {
        try {
            GeneralSoundsConfig.Reaction r = GeneralSoundsConfig.getMobReactions().get(mobId);
            if (r != null && r.enabled && r.speed > 0 && r.range > 0) {
                List<SoundGroupData> groups = SoundConfig.getEnabledSoundGroups();
                if (!groups.isEmpty()) {
                    Goal goal = new ReactToGeneralSoundGoal(mob, r.speed, (int) r.range, groups);
                    mob.goalSelector.addGoal(2, goal);
                    return goal;
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject ReactToGeneralSoundGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Goal injectGunfireGoal(MobEntity mob, String mobId) {
        try {
            GunfireConfig.Reaction r = GunfireConfig.getMobReactions().get(mobId);
            if (r != null && r.enabled && r.speed > 0.0D && r.range > 0.0D) {
                Goal goal = new ReactToGunfireGoal(mob, (double) r.speed, (double) r.range);
                mob.goalSelector.addGoal(2, goal);
                return goal;
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject ReactToGunfireGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void cleanupOldGoals(MobEntity mob) {
        try {
            InjectedGoals injected = INJECTED_GOALS.get(mob.getUUID());
            if (injected != null) {
                removeGoal(mob, injected.followGoal);
                removeGoal(mob, injected.runawayGoal);
                removeGoal(mob, injected.generalSoundGoal);
                removeGoal(mob, injected.gunfireGoal);
                INJECTED_GOALS.remove(mob.getUUID());
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error removing old goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void removeGoal(MobEntity mob, Goal goal) {
        if (goal != null) {
            mob.goalSelector.removeGoal(goal);
        }
    }

    private static class InjectedGoals {
        Goal followGoal;
        Goal runawayGoal;
        Goal generalSoundGoal;
        Goal gunfireGoal;

        boolean hasAnyGoal() {
            return followGoal != null || runawayGoal != null || generalSoundGoal != null || gunfireGoal != null;
        }
    }
}