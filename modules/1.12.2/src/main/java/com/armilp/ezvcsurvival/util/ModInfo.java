//package com.armilp.ezvcsurvival.util;
//
//import com.armilp.ezvcsurvival.EZVCSurvival;
//import net.minecraft.util.text.ITextComponent;
//import net.minecraft.util.text.TextComponentString;
//import net.minecraftforge.fml.common.Loader;
//import net.minecraftforge.fml.common.ModContainer;
//
//public class ModInfo {
//
//    public static ITextComponent getVersion() {
//        return new TextComponentString(EZVCSurvival.MOD_ID + "-v" + getVersionString());
//    }
//
//    public static String getVersionString() {
//        ModContainer modContainer = Loader.instance().getIndexedModList().get(EZVCSurvival.MOD_ID);
//        if (modContainer != null) {
//            return modContainer.getVersion();
//        }
//        return EZVCSurvival.VERSION;
//    }
//
//}