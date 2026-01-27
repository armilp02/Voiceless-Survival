package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import com.armilp.ezvcsurvival.mixins.GoalSelectorAccessor;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class MobGoalInjector {

    private static final String TAG = "ezvcsurvival:goals_added";
    private static final Set<EntityLiving> TRACKED_MOBS = Collections.newSetFromMap(new WeakHashMap<EntityLiving, Boolean>());
    private static final Map<UUID, InjectedGoals> INJECTED_GOALS = new WeakHashMap<UUID, InjectedGoals>();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (!(event.getEntity() instanceof EntityLiving)) {
            return;
        }

        EntityLiving mob = (EntityLiving) event.getEntity();
        World level = event.getWorld();

        if (level.isRemote) {
            return;
        }

        if (mob.isDead) {
            return;
        }

        try {
            if (!hasActiveGoals(mob)) {
                injectGoals(mob);

                NBTTagCompound data = mob.getEntityData();
                data.setBoolean(TAG, true);
                TRACKED_MOBS.add(mob);

                if (VoiceConfig.DEBUG) {
                    ResourceLocation id = getEntityId(mob);
                    System.out.println("[EZVCSurvival] Injected goals into: " + (id != null ? id : "unknown") + " (UUID=" + mob.getUniqueID() + ")");
                }
            } else {
                TRACKED_MOBS.add(mob);

                if (VoiceConfig.DEBUG) {
                    ResourceLocation id = getEntityId(mob);
                    System.out.println("[EZVCSurvival] Goals already present for: " + (id != null ? id : "unknown") + " (UUID=" + mob.getUniqueID() + ")");
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Error injecting goals on join: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static boolean hasActiveGoals(EntityLiving mob) {
        InjectedGoals injected = INJECTED_GOALS.get(mob.getUniqueID());
        if (injected != null && injected.hasAnyGoal()) {
            return verifyGoalsInSelector(mob, injected);
        }
        return false;
    }

    private static boolean verifyGoalsInSelector(EntityLiving mob, InjectedGoals injected) {
        Set<EntityAITasks.EntityAITaskEntry> taskEntries = ((GoalSelectorAccessor) mob.tasks).getTaskEntries();

        boolean hasFollowGoal = injected.followGoal == null ||
                taskEntries.stream().anyMatch(entry -> entry.action == injected.followGoal);
        boolean hasRunawayGoal = injected.runawayGoal == null ||
                taskEntries.stream().anyMatch(entry -> entry.action == injected.runawayGoal);
        boolean hasGeneralSoundGoal = injected.generalSoundGoal == null ||
                taskEntries.stream().anyMatch(entry -> entry.action == injected.generalSoundGoal);

        return hasFollowGoal && hasRunawayGoal && hasGeneralSoundGoal;
    }

    public static void refreshAll() {
        Iterator<EntityLiving> it = TRACKED_MOBS.iterator();
        while (it.hasNext()) {
            EntityLiving mob = it.next();
            if (mob == null || mob.isDead) {
                it.remove();
                continue;
            }
            refreshMob(mob);
        }
    }

    public static void refreshEntityId(String entityId) {
        for (EntityLiving mob : new ArrayList<EntityLiving>(TRACKED_MOBS)) {
            if (mob == null || mob.isDead) {
                continue;
            }
            ResourceLocation id = getEntityId(mob);
            if (id != null && id.toString().equals(entityId)) {
                refreshMob(mob);
            }
        }
    }

    private static void refreshMob(EntityLiving mob) {
        if (mob == null || mob.isDead) {
            return;
        }
        try {
            cleanupOldGoals(mob);
            injectGoals(mob);

            if (VoiceConfig.DEBUG) {
                ResourceLocation id = getEntityId(mob);
                System.out.println("[EZVCSurvival] Refreshed goals for: " + (id != null ? id : "unknown") + " (UUID=" + mob.getUniqueID() + ")");
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Error refreshing goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void injectGoals(EntityLiving mob) {
        if (mob == null || mob.isDead) {
            return;
        }

        ResourceLocation id = getEntityId(mob);
        if (id == null) {
            return;
        }
        String mobId = id.toString();

        InjectedGoals injectedGoals = new InjectedGoals();
        boolean isAnimal = mob instanceof EntityAnimal;

        if (EntityVoiceConfig.isEnabled()) {
            if (!isAnimal) {
                injectedGoals.followGoal = injectFollowVoiceGoal(mob, mobId);
            } else {
                injectedGoals.runawayGoal = injectRunawayVoiceGoal((EntityAnimal) mob, mobId);
            }
        }

        if (GeneralSoundsConfig.isEnabled()) {
            injectedGoals.generalSoundGoal = injectGeneralSoundGoal(mob, mobId);
        }

        INJECTED_GOALS.put(mob.getUniqueID(), injectedGoals);
    }

    private static EntityAIBase injectFollowVoiceGoal(EntityLiving mob, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                EntityAIBase goal = new FollowVoiceGoal((EntityCreature) mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000);
                mob.tasks.addTask(2, goal);
                return goal;
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Failed to inject FollowVoiceGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    private static EntityAIBase injectRunawayVoiceGoal(EntityAnimal animal, String mobId) {
        try {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(mobId);
            if (cfg != null && cfg.enabled && cfg.speed > 0 && cfg.range > 0) {
                EntityAIBase goal = new RunawayVoiceGoal(animal, cfg.speed, (int) cfg.range, cfg.threshold);
                animal.tasks.addTask(4, goal);
                return goal;
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Failed to inject RunawayVoiceGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    private static EntityAIBase injectGeneralSoundGoal(EntityLiving mob, String mobId) {
        try {
            GeneralSoundsConfig.Reaction r = GeneralSoundsConfig.getMobReactions().get(mobId);
            if (r != null && r.enabled && r.speed > 0 && r.range > 0) {
                List<SoundGroupData> groups = SoundConfig.getEnabledSoundGroups();
                if (!groups.isEmpty()) {
                    EntityAIBase goal = new ReactToGeneralSoundGoal((EntityCreature) mob, r.speed, (int) r.range, groups);
                    mob.tasks.addTask(2, goal);
                    return goal;
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Failed to inject ReactToGeneralSoundGoal for " + mobId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void cleanupOldGoals(EntityLiving mob) {
        try {
            InjectedGoals injected = INJECTED_GOALS.get(mob.getUniqueID());
            if (injected != null) {
                removeGoal(mob, injected.followGoal);
                removeGoal(mob, injected.runawayGoal);
                removeGoal(mob, injected.generalSoundGoal);
                INJECTED_GOALS.remove(mob.getUniqueID());
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Error removing old goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void removeGoal(EntityLiving mob, EntityAIBase goal) {
        if (goal != null) {
            mob.tasks.removeTask(goal);
        }
    }

    private static ResourceLocation getEntityId(EntityLiving mob) {
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(EntityList.getKey(mob.getClass()));
        return entry != null ? entry.getRegistryName() : null;
    }


    private static class InjectedGoals {
        EntityAIBase followGoal;
        EntityAIBase runawayGoal;
        EntityAIBase generalSoundGoal;

        boolean hasAnyGoal() {
            return followGoal != null || runawayGoal != null || generalSoundGoal != null;
        }
    }
}