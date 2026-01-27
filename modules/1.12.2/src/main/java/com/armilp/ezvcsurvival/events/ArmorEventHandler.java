package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArmorEventHandler {
    private static final Map<String, ArmorEffect> armorEffectsMap = new HashMap<String, ArmorEffect>();
    private static boolean initialized = false;

    private static void init() {
        if (initialized) return;
        Object configValue = VoiceConfig.ARMOR_EFFECTS;

        // Si es una lista
        if (configValue instanceof List) {
            List<?> configList = (List<?>) configValue;
            for (Object obj : configList) {
                String entry = obj.toString();
                parseEntry(entry);
            }
        }
        // Si es un String (puede ser una línea o múltiples líneas separadas)
        else if (configValue instanceof String) {
            String configString = (String) configValue;
            String[] entries = configString.split(";");
            for (String entry : entries) {
                parseEntry(entry);
            }
        }

        initialized = true;
    }

    private static void parseEntry(String entry) {
        String[] parts = entry.split("=");
        if (parts.length != 2) return;
        String itemId = parts[0].trim();
        String[] values = parts[1].split(",");
        if (values.length != 2) return;
        try {
            double speedMultiplier = Double.parseDouble(values[0].trim());
            double rangeMultiplier = Double.parseDouble(values[1].trim());
            armorEffectsMap.put(itemId, new ArmorEffect(speedMultiplier, rangeMultiplier));
        } catch (NumberFormatException e) {
            // Ignorar entradas malformadas
        }
    }

    public static double[] getArmorMultipliers(EntityPlayerMP player) {
        init();
        double speedMultiplier = 1.0;
        double rangeMultiplier = 1.0;

        EntityEquipmentSlot[] slots = new EntityEquipmentSlot[]{
                EntityEquipmentSlot.HEAD,
                EntityEquipmentSlot.CHEST,
                EntityEquipmentSlot.LEGS,
                EntityEquipmentSlot.FEET
        };

        for (EntityEquipmentSlot slot : slots) {
            ItemStack stack = player.getItemStackFromSlot(slot);
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