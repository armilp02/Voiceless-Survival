package com.armilp.ezvcsurvival.mixins;

import com.armilp.ezvcsurvival.goals.FollowVoiceGoal;
import com.armilp.ezvcsurvival.goals.RunawayVoiceGoal;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import com.armilp.ezvcsurvival.goals.ReactToGunfireGoal;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.data.SoundGroupData;
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
import java.util.Map;

@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Unique
    private boolean ezvcsurvival$goalsInjected = false;

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
            String mobIdString = mobId.toString();

            // FollowVoiceGoal
            Map<String, Map<String, Double>> mobVoiceConfigs = VoiceConfig.getMobVoiceConfigs();
            if (mobVoiceConfigs != null && !mobVoiceConfigs.isEmpty() && mobVoiceConfigs.containsKey(mobIdString)) {
                Map<String, Double> config = mobVoiceConfigs.get(mobIdString);
                if (config != null) {
                    double speed = config.getOrDefault("speed", 1.0);
                    double range = config.getOrDefault("range", 16.0);
                    double threshold = config.getOrDefault("threshold", -40.0);

                    if (speed > 0 && range > 0) {
                        FollowVoiceGoal voiceGoal = new FollowVoiceGoal(mob, speed, (int) range, threshold, 10000);
                        mob.goalSelector.addGoal(0, voiceGoal);

                        if (VoiceConfig.DEBUG.get()) {
                            System.out.println("[EZVCSurvival] Successfully injected FollowVoiceGoal for mob: " +
                                    mobIdString + " with params: speed=" + speed +
                                    ", range=" + (int) range + ", threshold=" + threshold);
                        }
                    } else {
                        if (VoiceConfig.DEBUG.get()) {
                            System.err.println("[EZVCSurvival] Invalid config parameters for mob " + mobIdString +
                                    ": speed=" + speed + ", range=" + range);
                        }
                    }
                }
            }

            // RunawayVoiceGoal para animales
            if (mob instanceof Animal animal) {
                Map<String, Map<String, Double>> animalVoiceConfigs = VoiceConfig.getAnimalVoiceConfigs();
                if (animalVoiceConfigs != null && animalVoiceConfigs.containsKey(mobIdString)) {
                    Map<String, Double> config = animalVoiceConfigs.get(mobIdString);
                    if (config != null) {
                        double speed = config.getOrDefault("speed", 1.0);
                        double range = config.getOrDefault("range", 16.0);
                        double threshold = config.getOrDefault("threshold", -40.0);

                        if (speed > 0 && range > 0) {
                            RunawayVoiceGoal runawayGoal = new RunawayVoiceGoal(animal, speed, (int) range, threshold);
                            animal.goalSelector.addGoal(4, runawayGoal);

                            if (VoiceConfig.DEBUG.get()) {
                                System.out.println("[EZVCSurvival] Successfully injected RunawayVoiceGoal for animal: " +
                                        mobIdString + " with params: speed=" + speed +
                                        ", range=" + (int) range + ", threshold=" + threshold);
                            }
                        }
                    }
                }
            }

            // ReactToGeneralSoundGoal
            Map<String, Object> generalConfig = SoundConfig.getGeneralSoundReaction(mobIdString);
            if (generalConfig != null) {
                double speed = generalConfig.get("speed") instanceof Number ?
                        ((Number) generalConfig.get("speed")).doubleValue() : 1.0;
                double rangeDouble = generalConfig.get("range") instanceof Number ?
                        ((Number) generalConfig.get("range")).doubleValue() : 16.0;
                int range = (int) rangeDouble;

                List<SoundGroupData> soundGroups = SoundConfig.getEnabledSoundGroups();
                if (!soundGroups.isEmpty()) {
                    mob.goalSelector.addGoal(2, new ReactToGeneralSoundGoal(mob, speed, range, soundGroups));

                    if (VoiceConfig.DEBUG.get()) {
                        System.out.println("[EZVCSurvival] Successfully injected ReactToGeneralSoundGoal for mob: " +
                                mobIdString + " with " + soundGroups.size() + " sound groups");
                    }
                }
            }

            // ReactToGunfireGoal
            Map<String, Object> gunfireConfig = SoundConfig.getGunfireSoundReaction(mobIdString);
            if (gunfireConfig != null) {
                double speed = gunfireConfig.get("speed") instanceof Number ?
                        ((Number) gunfireConfig.get("speed")).doubleValue() : 1.0;
                double rangeDouble = gunfireConfig.get("range") instanceof Number ?
                        ((Number) gunfireConfig.get("range")).doubleValue() : 20.0;
                int range = (int) rangeDouble;

                mob.goalSelector.addGoal(2, new ReactToGunfireGoal(mob, speed, range));

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Successfully injected ReactToGunfireGoal for mob: " +
                            mobIdString + " with params: speed=" + speed + ", range=" + range);
                }
            }

            ezvcsurvival$goalsInjected = true;

        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Failed to inject goals for mob " +
                        mob.getType().toString() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}