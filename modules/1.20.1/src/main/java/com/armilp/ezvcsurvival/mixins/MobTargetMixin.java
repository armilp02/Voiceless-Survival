package com.armilp.ezvcsurvival.mixins;

import com.armilp.ezvcsurvival.Plugin;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(Mob.class)
public abstract class MobTargetMixin {

    @Unique
    private boolean ezvcsurvival$isSpeedBoosted = false;
    @Unique
    private static final UUID VOICE_SPEED_MODIFIER_UUID = UUID.fromString("7E0292F2-9434-48D5-BE13-93E4BA5B5178");
    @Unique
    private int ezvcsurvival$boostCooldown = 0;
    @Unique
    private static final int BOOST_DURATION_TICKS = 60;

    @Inject(method = "tick", at = @At("HEAD"))
    private void ezvcsurvival$checkTargetAndVoice(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;

        if (mob.level().isClientSide) {
            return;
        }

        if (!EntityVoiceConfig.isEnabled() || !VoiceConfig.MOB_SPEED_BOOST_ENABLED.get()) {
            ezvcsurvival$removeSpeedBoost(mob);
            ezvcsurvival$boostCooldown = 0;
            return;
        }

        try {
            if (mob.getTarget() instanceof Player targetPlayer) {
                ezvcsurvival$handleTargetedPlayer(mob, targetPlayer);
            } else {
                if (ezvcsurvival$boostCooldown > 0) {
                    ezvcsurvival$boostCooldown--;
                }
                if (ezvcsurvival$boostCooldown <= 0) {
                    ezvcsurvival$removeSpeedBoost(mob);
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error in target voice check: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Unique
    private void ezvcsurvival$handleTargetedPlayer(Mob mob, Player targetPlayer) {
        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (mobId == null) return;

        String mobIdString = mobId.toString();
        double speedMultiplier = ezvcsurvival$getSpeedFromConfig(mobIdString);

        if (speedMultiplier <= 0.0) {
            ezvcsurvival$removeSpeedBoost(mob);
            ezvcsurvival$boostCooldown = 0;
            return;
        }

        boolean playerIsSpeaking = ezvcsurvival$isPlayerCurrentlySpeaking(targetPlayer);

        if (playerIsSpeaking) {
            if (!ezvcsurvival$isSpeedBoosted) {
                ezvcsurvival$applySpeedBoost(mob, mobIdString, speedMultiplier);
            }
            ezvcsurvival$boostCooldown = BOOST_DURATION_TICKS;
        } else {
            if (ezvcsurvival$boostCooldown > 0) {
                ezvcsurvival$boostCooldown--;
            }

            if (ezvcsurvival$boostCooldown <= 0 && ezvcsurvival$isSpeedBoosted) {
                ezvcsurvival$removeSpeedBoost(mob);
            }
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
    private void ezvcsurvival$applySpeedBoost(Mob mob, String mobIdString, double speedMultiplier) {
        AttributeInstance speedAttribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) {
            return;
        }

        speedAttribute.removeModifier(VOICE_SPEED_MODIFIER_UUID);

        double baseSpeed = speedAttribute.getBaseValue();
        double boostAmount = baseSpeed * (speedMultiplier - 1.0);

        if (boostAmount <= 0.0) {
            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Invalid speed multiplier for " + mobIdString +
                        ": " + speedMultiplier + " (must be > 1.0)");
            }
            return;
        }

        AttributeModifier speedModifier = new AttributeModifier(
                VOICE_SPEED_MODIFIER_UUID,
                "Voice targeting speed boost",
                boostAmount,
                AttributeModifier.Operation.ADDITION
        );

        speedAttribute.addPermanentModifier(speedModifier);
        ezvcsurvival$isSpeedBoosted = true;

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Applied speed boost to " + mobIdString +
                    " targeting speaking player. Base Speed: " + baseSpeed +
                    ", Multiplier: " + speedMultiplier +
                    ", Boost: +" + boostAmount +
                    ", Final Speed: " + speedAttribute.getValue());
        }
    }

    @Unique
    private void ezvcsurvival$removeSpeedBoost(Mob mob) {
        if (!ezvcsurvival$isSpeedBoosted) return;

        AttributeInstance speedAttribute = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute == null) {
            return;
        }

        speedAttribute.removeModifier(VOICE_SPEED_MODIFIER_UUID);
        ezvcsurvival$isSpeedBoosted = false;

        if (VoiceConfig.DEBUG.get()) {
            ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
            System.out.println("[EZVCSurvival] Removed speed boost from " +
                    (mobId != null ? mobId.toString() : "unknown") +
                    ", Speed now: " + speedAttribute.getValue());
        }
    }
}