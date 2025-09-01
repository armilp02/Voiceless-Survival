package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.awt.Desktop;
import java.io.File;
import java.util.function.Supplier;

public class OpenConfigEditorPacket {
    
    public OpenConfigEditorPacket() {}
    
    public OpenConfigEditorPacket(FriendlyByteBuf buf) {}
    
    public void encode(FriendlyByteBuf buf) {}
    
    public static OpenConfigEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenConfigEditorPacket(buf);
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    net.minecraft.client.Minecraft.getInstance().setScreen(
                        new ConfigEditorScreen()
                    );
                } catch (Exception e) {
                    try {
                        File configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("ezvcsurvival").toFile();
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().open(configDir);
                        }
                    } catch (Exception ex) {
                        com.armilp.ezvcsurvival.EZVCSurvival.LOGGER.warn("No se pudo abrir la configuración: {}", ex.getMessage());
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
