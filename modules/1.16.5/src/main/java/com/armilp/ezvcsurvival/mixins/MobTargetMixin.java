package com.armilp.ezvcsurvival.mixins;

import com.armilp.ezvcsurvival.Plugin;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobTargetMixin {

    @Unique
    private boolean ezvcsurvival$isSpeedBoosted = false;
    @Unique
    private static final UUID VOICE_SPEED_MODIFIER_UUID = UUID.fromString("7E0292F2-9434-48D5-BE13-93E4BA5B5178");
    @Unique
    private double ezvcsurvival$originalSpeed = -1.0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ezvcsurvival$checkTargetAndVoice(CallbackInfo ci) {
        MobEntity mob = (MobEntity) (Object) this;

        if (mob.level.isClientSide) {
            return;
        }

        if (!EntityVoiceConfig.isEnabled() || !VoiceConfig.MOB_SPEED_BOOST_ENABLED.get()) {
            ezvcsurvival$removeSpeedBoost(mob);
            return;
        }

        try {
            if (mob.getTarget() instanceof PlayerEntity) {
                PlayerEntity targetPlayer = (PlayerEntity) mob.getTarget();
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
    private void ezvcsurvival$handleTargetedPlayer(MobEntity mob, PlayerEntity targetPlayer) {
        ResourceLocation mobId = ForgeRegistries.ENTITIES.getKey(mob.getType());
        String mobIdString = mobId.toString();

        double speedFromConfig = ezvcsurvival$getSpeedFromConfig(mobIdString);
        if (speedFromConfig == -1.0) {
            return;
        }

        boolean playerIsSpeaking = ezvcsurvival$isPlayerCurrentlySpeaking(targetPlayer);

        if (playerIsSpeaking && !ezvcsurvival$isSpeedBoosted) {
            ezvcsurvival$applySpeedBoost(mob, mobIdString, speedFromConfig);
        } else if (!playerIsSpeaking && ezvcsurvival$isSpeedBoosted) {
            ezvcsurvival$removeSpeedBoost(mob);
        }
    }

    @Unique
    private boolean ezvcsurvival$isPlayerCurrentlySpeaking(PlayerEntity player) {
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
    private void ezvcsurvival$applySpeedBoost(MobEntity mob, String mobIdString, double speedFromConfig) {
        if (ezvcsurvival$originalSpeed == -1.0) {
            ezvcsurvival$originalSpeed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        }

        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) == null) {
            return;
        }
        mob.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(VOICE_SPEED_MODIFIER_UUID);

        double boostAmount = ezvcsurvival$originalSpeed * speedFromConfig * 0.5;
        AttributeModifier speedModifier = new AttributeModifier(
                VOICE_SPEED_MODIFIER_UUID,
                "Voice targeting speed boost",
                boostAmount,
                AttributeModifier.Operation.ADDITION
        );

        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) == null) {
            return;
        }
        mob.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(speedModifier);
        ezvcsurvival$isSpeedBoosted = true;

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Applied speed boost to " + mobIdString +
                    " targeting speaking player. Original: " + ezvcsurvival$originalSpeed +
                    " Config Speed: " + speedFromConfig + " Boost: +" + boostAmount);
        }
    }

    @Unique
    private void ezvcsurvival$removeSpeedBoost(MobEntity mob) {
        if (!ezvcsurvival$isSpeedBoosted) return;

        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) == null) {
            return;
        }
        mob.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(VOICE_SPEED_MODIFIER_UUID);
        ezvcsurvival$isSpeedBoosted = false;

        if (VoiceConfig.DEBUG.get()) {
            ResourceLocation mobId = ForgeRegistries.ENTITIES.getKey(mob.getType());
            System.out.println("[EZVCSurvival] Removed speed boost from " + mobId.toString());
        }
    }
}