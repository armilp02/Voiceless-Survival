package com.armilp.ezvcsurvival.mixins;

import com.armilp.ezvcsurvival.Plugin;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobTargetMixin {

    @Unique
    private boolean ezvcsurvival$isSpeedBoosted = false;

    @Unique
    private static final ResourceLocation VOICE_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("ezvcsurvival", "voice_speed_boost");

    @Unique
    private double ezvcsurvival$originalSpeed = -1.0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ezvcsurvival$checkTargetAndVoice(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide()) return;

        if (!EntityVoiceConfig.isEnabled() || !VoiceConfig.MOB_SPEED_BOOST_ENABLED.get()) {
            ezvcsurvival$removeSpeedBoost(mob);
            return;
        }

        try {
            if (mob.getTarget() instanceof Player targetPlayer) {
                ezvcsurvival$handleTargetedPlayer(mob, targetPlayer);
            } else {
                ezvcsurvival$removeSpeedBoost(mob);
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error in target voice check: " + e.getMessage());
            }
        }
    }

    @Unique
    private void ezvcsurvival$handleTargetedPlayer(Mob mob, Player targetPlayer) {
        ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

        double speedFromConfig = ezvcsurvival$getSpeedFromConfig(mobId.toString());
        if (speedFromConfig == -1.0) return;

        boolean playerIsSpeaking = ezvcsurvival$isPlayerCurrentlySpeaking(targetPlayer);

        if (playerIsSpeaking && !ezvcsurvival$isSpeedBoosted) {
            ezvcsurvival$applySpeedBoost(mob, speedFromConfig);
        } else if (!playerIsSpeaking && ezvcsurvival$isSpeedBoosted) {
            ezvcsurvival$removeSpeedBoost(mob);
        }
    }

    @Unique
    private boolean ezvcsurvival$isPlayerCurrentlySpeaking(Player player) {
        return Plugin.getLastSoundLocation(player.blockPosition(), 64.0, -20.0) != null;
    }

    @Unique
    private double ezvcsurvival$getSpeedFromConfig(String mobIdString) {
        EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(mobIdString);
        if (cfg == null) cfg = EntityVoiceConfig.getAnimal(mobIdString);
        if (cfg != null && cfg.enabled) {
            return cfg.speed;
        }
        return -1.0;
    }

    @Unique
    private void ezvcsurvival$applySpeedBoost(Mob mob, double speedFromConfig) {
        AttributeInstance inst = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;

        if (ezvcsurvival$originalSpeed == -1.0) {
            ezvcsurvival$originalSpeed = inst.getValue();
        }

        inst.removeModifier(VOICE_SPEED_MODIFIER_ID);

        double boostAmount = ezvcsurvival$originalSpeed * speedFromConfig * 0.5;
        AttributeModifier speedModifier = new AttributeModifier(
                VOICE_SPEED_MODIFIER_ID,
                boostAmount,
                AttributeModifier.Operation.ADD_VALUE
        );

        inst.addPermanentModifier(speedModifier);
        ezvcsurvival$isSpeedBoosted = true;

        if (VoiceConfig.DEBUG.get()) {
            ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            System.out.println("[EZVCSurvival] Applied speed boost to " + mobId +
                    " targeting speaking player. Original: " + ezvcsurvival$originalSpeed +
                    " Config Speed: " + speedFromConfig + " Boost: +" + boostAmount);
        }
    }

    @Unique
    private void ezvcsurvival$removeSpeedBoost(Mob mob) {
        if (!ezvcsurvival$isSpeedBoosted) return;

        AttributeInstance inst = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;

        inst.removeModifier(VOICE_SPEED_MODIFIER_ID);
        ezvcsurvival$isSpeedBoosted = false;

        if (VoiceConfig.DEBUG.get()) {
            ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            System.out.println("[EZVCSurvival] Removed speed boost from " + mobId);
        }
    }
}