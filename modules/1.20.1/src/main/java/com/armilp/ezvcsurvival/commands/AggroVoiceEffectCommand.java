package com.armilp.ezvcsurvival.commands;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

public class AggroVoiceEffectCommand {

    public static void applyEffect(ServerPlayer player) {
        ResourceLocation effectId = ResourceLocation.fromNamespaceAndPath("quiet_place", "agrovoiceeffect");
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) {
            return;
        }

        MobEffectInstance instance = new MobEffectInstance(effect, 40, 1, false, false);
        player.addEffect(instance);
    }
}