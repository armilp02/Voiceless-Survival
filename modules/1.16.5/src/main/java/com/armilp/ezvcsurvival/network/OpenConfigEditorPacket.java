package com.armilp.ezvcsurvival.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenConfigEditorPacket {

    public OpenConfigEditorPacket() {
    }

    public OpenConfigEditorPacket(PacketBuffer buf) {
    }

    public static void encode(OpenConfigEditorPacket msg, PacketBuffer buf) {
    }

    public static OpenConfigEditorPacket decode(PacketBuffer buf) {
        return new OpenConfigEditorPacket(buf);
    }

    public static void handle(OpenConfigEditorPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> new Runnable() {
                    @Override
                    public void run() {
                        ClientPacketHandlers.handleOpenConfigEditor();
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}