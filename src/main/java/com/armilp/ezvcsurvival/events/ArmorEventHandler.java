package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.ArmorEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArmorEventHandler {
    private static final Map<String, ArmorEffect> armorEffectsMap = new HashMap<>();
    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        List<? extends String> configList = VoiceConfig.ARMOR_EFFECTS.get();
        for (String entry : configList) {
            String[] parts = entry.split("=");
            if (parts.length != 2) continue;
            String itemId = parts[0].trim();
            String[] values = parts[1].split(",");
            if (values.length != 2) continue;
            try {
                double speedMultiplier = Double.parseDouble(values[0].trim());
                double rangeMultiplier = Double.parseDouble(values[1].trim());
                armorEffectsMap.put(itemId, new ArmorEffect(speedMultiplier, rangeMultiplier));
            } catch (NumberFormatException ignored) {
            }
        }
        initialized = true;
    }


    public static double[] getArmorMultipliers(Player player) {
        init();
        double speedMultiplier = 1.0;
        double rangeMultiplier = 1.0;

        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (armorEffectsMap.containsKey(id.toString())) {
                    ArmorEffect effect = armorEffectsMap.get(id.toString());
                    speedMultiplier *= effect.speedMultiplier;
                    rangeMultiplier *= effect.rangeMultiplier;
                }
            }
        }
        return new double[]{speedMultiplier, rangeMultiplier};
    }
}
