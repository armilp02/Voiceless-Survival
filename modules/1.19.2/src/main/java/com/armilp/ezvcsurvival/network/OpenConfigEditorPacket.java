package com.armilp.ezvcsurvival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenConfigEditorPacket {

    public OpenConfigEditorPacket() {
    }

    public OpenConfigEditorPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static OpenConfigEditorPacket decode(FriendlyByteBuf buf) {
        return new OpenConfigEditorPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientPacketHandlers::handleOpenConfigEditor);
        });
        ctx.get().setPacketHandled(true);
    }
}
