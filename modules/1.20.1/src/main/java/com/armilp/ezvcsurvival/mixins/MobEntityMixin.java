package com.armilp.ezvcsurvival.mixins;

import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.goals.ReactToGunfireGoal;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import com.armilp.ezvcsurvival.util.IGoalRefresher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(value = Mob.class, priority = 1100)
public abstract class MobEntityMixin implements IGoalRefresher {

    @Unique
    private boolean ezvcsurvival$goalsInjected = false;
    @Unique
    private String ezvcsurvival$cachedMobId = null;

    @Unique
    private Goal ezvcsurvival$followGoal = null;
    @Unique
    private Goal ezvcsurvival$runawayGoal = null;
    @Unique
    private Goal ezvcsurvival$generalSoundGoal = null;
    @Unique
    private Goal ezvcsurvival$gunfireGoal = null;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ezvcsurvival$injectGoalsAfterRegister(CallbackInfo ci) {
        ezvcsurvival$tryInjectGoals();
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void ezvcsurvival$injectGoalsOnSpawn(ServerLevelAccessor p_21434_, DifficultyInstance p_21435_,
                                                 MobSpawnType p_21436_, @Nullable SpawnGroupData p_21437_,
                                                 @Nullable CompoundTag p_21438_, CallbackInfoReturnable<SpawnGroupData> cir) {
        ezvcsurvival$tryInjectGoals();
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void ezvcsurvival$injectGoalsOnLoad(CompoundTag tag, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!mob.level().isClientSide) {
            ezvcsurvival$goalsInjected = false;
            ezvcsurvival$tryInjectGoals();
        }
    }

    @Unique
    private void ezvcsurvival$tryInjectGoals() {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide || ezvcsurvival$goalsInjected) {
            return;
        }

        if (ezvcsurvival$cachedMobId == null) {
            ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
            if (mobId == null) {
                ezvcsurvival$goalsInjected = true;
                return;
            }
            ezvcsurvival$cachedMobId = mobId.toString();
        }

        try {
            ezvcsurvival$injectAllGoals(mob);
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error injecting goals for " + ezvcsurvival$cachedMobId + ": " + e.getMessage());
                e.printStackTrace();
            }
            ezvcsurvival$goalsInjected = true;
        }
    }

    @Unique
    private void ezvcsurvival$injectAllGoals(Mob mob) {
        if (ezvcsurvival$goalsInjected) return;

        boolean isAnimal = mob instanceof Animal;
        int goalsAdded = 0;

        if (EntityVoiceConfig.isEnabled()) {
            if (!isAnimal) {
                if (ezvcsurvival$injectFollowVoiceGoal(mob)) goalsAdded++;
            } else {
                if (ezvcsurvival$injectRunawayVoiceGoal((Animal) mob)) goalsAdded++;
            }
        }

        if (GeneralSoundsConfig.isEnabled()) {
            if (ezvcsurvival$injectGeneralSoundGoal(mob)) goalsAdded++;
        }

        if (GunfireConfig.isEnabled()) {
            if (ezvcsurvival$injectGunfireGoal(mob)) goalsAdded++;
        }

        if (VoiceConfig.DEBUG.get() && goalsAdded > 0) {
            System.out.println("[DEBUG] Injected " + goalsAdded + " goals for " + ezvcsurvival$cachedMobId);
        }

        ezvcsurvival$goalsInjected = true;
    }

    @Unique
    private boolean ezvcsurvival$injectFollowVoiceGoal(Mob mob) {
        EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(ezvcsurvival$cachedMobId);
        if (cfg == null || !cfg.enabled || cfg.speed <= 0 || cfg.range <= 0) return false;

        ezvcsurvival$followGoal = new FollowVoiceGoal(mob, cfg.speed, (int) cfg.range, cfg.threshold, 10000);
        mob.goalSelector.addGoal(0, ezvcsurvival$followGoal);

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[DEBUG] Added FollowVoiceGoal to " + ezvcsurvival$cachedMobId +
                    " (speed: " + cfg.speed + ", range: " + cfg.range + ", threshold: " + cfg.threshold + ")");
        }
        return true;
    }

    @Unique
    private boolean ezvcsurvival$injectRunawayVoiceGoal(Animal animal) {
        EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getAnimal(ezvcsurvival$cachedMobId);
        if (cfg == null || !cfg.enabled || cfg.speed <= 0 || cfg.range <= 0) return false;

        ezvcsurvival$runawayGoal = new RunawayVoiceGoal(animal, cfg.speed, (int) cfg.range, cfg.threshold);
        animal.goalSelector.addGoal(4, ezvcsurvival$runawayGoal);
        return true;
    }

    @Unique
    private boolean ezvcsurvival$injectGeneralSoundGoal(Mob mob) {
        GeneralSoundsConfig.Reaction reaction = GeneralSoundsConfig.getMobReactions().get(ezvcsurvival$cachedMobId);
        if (reaction == null || !reaction.enabled || reaction.speed <= 0 || reaction.range <= 0) return false;

        List<SoundGroupData> soundGroups = SoundConfig.getEnabledSoundGroups();
        if (soundGroups.isEmpty()) return false;

        ezvcsurvival$generalSoundGoal = new ReactToGeneralSoundGoal(mob, reaction.speed, (int) reaction.range);
        mob.goalSelector.addGoal(0, ezvcsurvival$generalSoundGoal);
        return true;
    }

    @Unique
    private boolean ezvcsurvival$injectGunfireGoal(Mob mob) {
        GunfireConfig.Reaction reaction = GunfireConfig.getMobReactions().get(ezvcsurvival$cachedMobId);
        if (reaction == null || !reaction.enabled || reaction.speed <= 0 || reaction.range <= 0) {
            return false;
        }

        ezvcsurvival$gunfireGoal = new ReactToGunfireGoal(mob, reaction.speed, (int) reaction.range);
        mob.goalSelector.addGoal(0, ezvcsurvival$gunfireGoal);
        return true;
    }

    @Unique
    private void ezvcsurvival$removeGoal(Mob mob, Goal goal) {
        if (goal != null) {
            mob.goalSelector.removeGoal(goal);
        }
    }

    @Unique
    private void ezvcsurvival$cleanupOldGoals(Mob mob) {
        ezvcsurvival$removeGoal(mob, ezvcsurvival$followGoal);
        ezvcsurvival$removeGoal(mob, ezvcsurvival$runawayGoal);
        ezvcsurvival$removeGoal(mob, ezvcsurvival$generalSoundGoal);
        ezvcsurvival$removeGoal(mob, ezvcsurvival$gunfireGoal);

        ezvcsurvival$followGoal = null;
        ezvcsurvival$runawayGoal = null;
        ezvcsurvival$generalSoundGoal = null;
        ezvcsurvival$gunfireGoal = null;
    }

    @Override
    public void ezvcsurvival$RefreshGoals() {
        Mob mob = (Mob) (Object) this;
        if (mob.level().isClientSide) return;

        try {
            ezvcsurvival$cleanupOldGoals(mob);
            ezvcsurvival$goalsInjected = false;
            ezvcsurvival$injectAllGoals(mob);
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error refreshing goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}