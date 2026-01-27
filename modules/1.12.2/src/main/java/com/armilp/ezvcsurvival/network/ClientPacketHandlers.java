package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.io.File;

@SideOnly(Side.CLIENT)
public class ClientPacketHandlers {
    public static void handleOpenConfigEditor() {
        try {
            Minecraft.getMinecraft().displayGuiScreen(new ConfigEditorScreen());
        } catch (Exception e) {
            try {
                File configDir = new File(FMLCommonHandler.instance().getMinecraftServerInstance().getDataDirectory(), "config/ezvcsurvival");
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(configDir);
                }
            } catch (Exception ex) {
                EZVCSurvival.LOGGER.warn("Failed to open the configuration");
            }
        }
    }
}