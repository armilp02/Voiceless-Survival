package com.armilp.ezvcsurvival.mixins;

import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.goals.*;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.util.IGoalRefresher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Mob.class)
public abstract class MobEntityMixin implements IGoalRefresher {

    @Unique private boolean ezvcsurvival$goalsInjected = false;

    @Unique private FollowVoiceGoal ezvcsurvival$followGoal = null;
    @Unique private RunawayVoiceGoal ezvcsurvival$runawayGoal = null;
    @Unique private ReactToGeneralSoundGoal ezvcsurvival$generalSoundGoal = null;
    @Unique private ReactToGunfireGoal ezvcsurvival$gunfireGoal = null;


    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ezvcsurvival$injectGoalsAfterRegister(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide || ezvcsurvival$goalsInjected) {
            return;
        }

        try {
            ezvcsurvival$injectAllGoals(mob);
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error injecting goals in registerGoals: " + e.getMessage());
            }
        }
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void ezvcsurvival$injectGoalsOnSpawn(ServerLevelAccessor p_21434_, DifficultyInstance p_21435_,
                                                 MobSpawnType p_21436_, @Nullable SpawnGroupData p_21437_,
                                                 @Nullable CompoundTag p_21438_, CallbackInfoReturnable<SpawnGroupData> cir) {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide || ezvcsurvival$goalsInjected) {
            return;
        }

        try {
            ezvcsurvival$injectAllGoals(mob);
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error injecting goals in finalizeSpawn: " + e.getMessage());
            }
        }
    }

    @Inject(method = "checkDespawn", at = @At("HEAD"))
    private void ezvcsurvival$ensureGoalsInjected(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide || ezvcsurvival$goalsInjected) {
            return;
        }

        try {
            ezvcsurvival$injectAllGoals(mob);
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error ensuring goals in checkDespawn: " + e.getMessage());
            }
        }
    }

    @Unique
    private void ezvcsurvival$injectAllGoals(Mob mob) {
        if (ezvcsurvival$goalsInjected) {
            return;
        }

        try {
            ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (mobId == null) {
                ezvcsurvival$goalsInjected = true;
                return;
            }
            String mobIdString = mobId.toString();

            // FollowVoiceGoal
            if (EntityVoiceConfig.isEnabled()) {
                if (!(mob instanceof Animal)) {
                    EntityVoiceConfig.EntityConfig voiceCfg = EntityVoiceConfig.getMonster(mobIdString);
                    if (voiceCfg != null && voiceCfg.enabled) {
                        double speed = voiceCfg.speed;
                        double range = voiceCfg.range;
                        double threshold = voiceCfg.threshold;

                        if (speed > 0 && range > 0) {
                            FollowVoiceGoal voiceGoal = new FollowVoiceGoal(mob, speed, (int) range, threshold, 10000);
                            ezvcsurvival$followGoal = voiceGoal;
                            mob.goalSelector.addGoal(0, voiceGoal);
                        }
                    }
                }
            }

            // RunawayVoiceGoal
            if (EntityVoiceConfig.isEnabled()) {
                if (mob instanceof Animal animal) {
                    EntityVoiceConfig.EntityConfig animalCfg = EntityVoiceConfig.getAnimal(mobIdString);
                    if (animalCfg != null && animalCfg.enabled) {
                        double speed = animalCfg.speed;
                        double range = animalCfg.range;
                        double threshold = animalCfg.threshold;

                        if (speed > 0 && range > 0) {
                            RunawayVoiceGoal runawayGoal = new RunawayVoiceGoal(animal, speed, (int) range, threshold);
                            ezvcsurvival$runawayGoal = runawayGoal;
                            animal.goalSelector.addGoal(4, runawayGoal);
                        }
                    }
                }
            }

            // ReactToGeneralSoundGoal
            if (GeneralSoundsConfig.isEnabled()) {
                GeneralSoundsConfig.Reaction generalReaction = GeneralSoundsConfig.getMobReactions().get(mobIdString);
                if (generalReaction != null && generalReaction.enabled) {
                    double speed = generalReaction.speed;
                    double range = generalReaction.range;

                    if (speed > 0 && range > 0) {
                        List<SoundGroupData> soundGroups = SoundConfig.getEnabledSoundGroups();
                        if (!soundGroups.isEmpty()) {
                            ReactToGeneralSoundGoal gGoal = new ReactToGeneralSoundGoal(mob, speed, (int) range, soundGroups);
                            ezvcsurvival$generalSoundGoal = gGoal;
                            mob.goalSelector.addGoal(2, gGoal);
                        }
                    }
                }
            }

            // ReactToGunfireGoal
            if (GunfireConfig.isEnabled()) {
                GunfireConfig.Reaction gunfireReaction = GunfireConfig.getMobReactions().get(mobIdString);
                if (gunfireReaction != null && gunfireReaction.enabled) {
                    double speed = gunfireReaction.speed;
                    double range = gunfireReaction.range;

                    if (VoiceConfig.DEBUG.get()) {
                        System.out.println("[EZVCSurvival] Applying Gunfire config for " + mobIdString +
                                ": enabled=" + gunfireReaction.enabled +
                                ", speed=" + speed +
                                ", range=" + range);
                    }

                    if (speed > 0 && range > 0) {
                        ReactToGunfireGoal gunGoal = new ReactToGunfireGoal(mob, speed, (int) range);
                        ezvcsurvival$gunfireGoal = gunGoal;
                        mob.goalSelector.addGoal(2, gunGoal);
                    }
                } else {
                    if (VoiceConfig.DEBUG.get()) {
                        System.out.println("[EZVCSurvival] Gunfire reaction not found or disabled for: " + mobIdString);
                    }
                }
            }

            ezvcsurvival$goalsInjected = true;

        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error injecting goals: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }
    
    @Unique
    private void ezvcsurvival$cleanupOldGoals(Mob mob) {
        try {
            // Remover goals antiguos de forma más robusta
            if (ezvcsurvival$followGoal != null) {
                mob.goalSelector.removeGoal(ezvcsurvival$followGoal);
                ezvcsurvival$followGoal = null;
            }
            if (ezvcsurvival$runawayGoal != null) {
                mob.goalSelector.removeGoal(ezvcsurvival$runawayGoal);
                ezvcsurvival$runawayGoal = null;
            }
            if (ezvcsurvival$generalSoundGoal != null) {
                mob.goalSelector.removeGoal(ezvcsurvival$generalSoundGoal);
                ezvcsurvival$generalSoundGoal = null;
            }
            if (ezvcsurvival$gunfireGoal != null) {
                mob.goalSelector.removeGoal(ezvcsurvival$gunfireGoal);
                ezvcsurvival$gunfireGoal = null;
            }

            ezvcsurvival$goalsInjected = false;
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error cleaning old goals: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void ezvcsurvival$RefreshGoals() {
        Mob mob = (Mob) (Object) this;
        if (mob.level().isClientSide) return;
        
        try {
            ezvcsurvival$goalsInjected = false;
            ezvcsurvival$cleanupOldGoals(mob);

            ezvcsurvival$injectAllGoals(mob);
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error in forced refresh: " + e.getMessage());
            }
        }
    }

}