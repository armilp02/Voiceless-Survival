package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

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
            } catch (NumberFormatException e) {
            }
        }
        initialized = true;
    }

    public static double[] getArmorMultipliers(ServerPlayerEntity player) {
        init();
        double speedMultiplier = 1.0;
        double rangeMultiplier = 1.0;

        EquipmentSlotType[] slots = new EquipmentSlotType[]{
                EquipmentSlotType.HEAD,
                EquipmentSlotType.CHEST,
                EquipmentSlotType.LEGS,
                EquipmentSlotType.FEET
        };

        for (EquipmentSlotType slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (id != null && armorEffectsMap.containsKey(id.toString())) {
                    ArmorEffect effect = armorEffectsMap.get(id.toString());
                    speedMultiplier *= effect.speedMultiplier();
                    rangeMultiplier *= effect.rangeMultiplier();
                }
            }
        }
        return new double[]{speedMultiplier, rangeMultiplier};
    }

    private static class ArmorEffect {
        private final double speedMultiplier;
        private final double rangeMultiplier;

        public ArmorEffect(double speedMultiplier, double rangeMultiplier) {
            this.speedMultiplier = speedMultiplier;
            this.rangeMultiplier = rangeMultiplier;
        }

        public double speedMultiplier() {
            return speedMultiplier;
        }

        public double rangeMultiplier() {
            return rangeMultiplier;
        }
    }
}