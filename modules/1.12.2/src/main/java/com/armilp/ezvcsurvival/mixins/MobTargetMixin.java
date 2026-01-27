package com.armilp.ezvcsurvival.mixins;

import com.armilp.ezvcsurvival.Plugin;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(EntityLiving.class)
public abstract class MobTargetMixin {

    @Unique
    private boolean ezvcsurvival$isSpeedBoosted = false;
    @Unique
    private static final UUID VOICE_SPEED_MODIFIER_UUID = UUID.fromString("7E0292F2-9434-48D5-BE13-93E4BA5B5178");
    @Unique
    private double ezvcsurvival$originalSpeed = -1.0;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void ezvcsurvival$checkTargetAndVoice(CallbackInfo ci) {
        EntityLiving mob = (EntityLiving) (Object) this;

        if (mob.world.isRemote) {
            return;
        }

        if (!EntityVoiceConfig.isEnabled() || !VoiceConfig.MOB_SPEED_BOOST_ENABLED) {
            ezvcsurvival$removeSpeedBoost(mob);
            return;
        }

        try {
            if (mob.getAttackTarget() instanceof EntityPlayer) {
                EntityPlayer targetPlayer = (EntityPlayer) mob.getAttackTarget();
                ezvcsurvival$handleTargetedPlayer(mob, targetPlayer);
            } else {
                ezvcsurvival$removeSpeedBoost(mob);
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Error in target voice check: " + e.getMessage());
            }
        }
    }

    @Unique
    private void ezvcsurvival$handleTargetedPlayer(EntityLiving mob, EntityPlayer targetPlayer) {
        ResourceLocation mobId = ezvcsurvival$getEntityId(mob);
        if (mobId == null) {
            return;
        }
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
    private boolean ezvcsurvival$isPlayerCurrentlySpeaking(EntityPlayer player) {
        return Plugin.getLastSoundLocation(player.getPosition(), 64.0, -20.0) != null;
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
    private void ezvcsurvival$applySpeedBoost(EntityLiving mob, String mobIdString, double speedFromConfig) {
        if (ezvcsurvival$originalSpeed == -1.0) {
            IAttributeInstance speedAttribute = mob.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                ezvcsurvival$originalSpeed = speedAttribute.getAttributeValue();
            }
        }

        IAttributeInstance speedAttribute = mob.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttribute == null) {
            return;
        }
        speedAttribute.removeModifier(VOICE_SPEED_MODIFIER_UUID);

        double boostAmount = ezvcsurvival$originalSpeed * speedFromConfig * 0.5;
        AttributeModifier speedModifier = new AttributeModifier(
                VOICE_SPEED_MODIFIER_UUID,
                "Voice targeting speed boost",
                boostAmount,
                0 // Operation.ADDITION = 0 en 1.12.2
        );

        speedAttribute.applyModifier(speedModifier);
        ezvcsurvival$isSpeedBoosted = true;

        if (VoiceConfig.DEBUG) {
            System.out.println("[EZVCSurvival] Applied speed boost to " + mobIdString +
                    " targeting speaking player. Original: " + ezvcsurvival$originalSpeed +
                    " Config Speed: " + speedFromConfig + " Boost: +" + boostAmount);
        }
    }

    @Unique
    private void ezvcsurvival$removeSpeedBoost(EntityLiving mob) {
        if (!ezvcsurvival$isSpeedBoosted) return;

        IAttributeInstance speedAttribute = mob.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttribute == null) {
            return;
        }
        speedAttribute.removeModifier(VOICE_SPEED_MODIFIER_UUID);
        ezvcsurvival$isSpeedBoosted = false;

        if (VoiceConfig.DEBUG) {
            ResourceLocation mobId = ezvcsurvival$getEntityId(mob);
            System.out.println("[EZVCSurvival] Removed speed boost from " + (mobId != null ? mobId.toString() : "unknown"));
        }
    }

    @Unique
    private ResourceLocation ezvcsurvival$getEntityId(EntityLiving mob) {
        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValuesCollection()) {
            if (entry.getEntityClass() == mob.getClass()) {
                return entry.getRegistryName();
            }
        }
        return null;
    }
}