package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLPaths;

import java.awt.Desktop;
import java.io.File;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandlers {

    public static void handleOpenConfigEditor() {
        try {
            Minecraft.getInstance().setScreen(new ConfigEditorScreen());
        } catch (Exception e) {
            try {
                File configDir = FMLPaths.CONFIGDIR.get().resolve("ezvcsurvival").toFile();
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(configDir);
                }
            } catch (Exception ex) {
                EZVCSurvival.LOGGER.warn("Failed to open the configuration: {}", ex.getMessage());
            }
        }
    }
}