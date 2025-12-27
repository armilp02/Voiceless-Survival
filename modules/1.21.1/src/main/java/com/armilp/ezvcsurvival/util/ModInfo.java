package com.armilp.ezvcsurvival.util;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

public class ModInfo {

    public static Component getVersion() {
        return Component.literal(EZVCSurvival.MOD_ID + "-v" + getVersionString());
    }

    public static String getVersionString() {
        ModContainer modContainer = ModList.get().getModContainerById(EZVCSurvival.MOD_ID).orElse(null);
        if (modContainer != null) {
            return modContainer.getModInfo().getVersion().toString();
        }
        return "2.0.0";
    }

}
