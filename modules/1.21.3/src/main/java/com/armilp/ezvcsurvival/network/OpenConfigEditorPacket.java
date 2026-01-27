package com.armilp.ezvcsurvival.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

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

    public void handle(CustomPayloadEvent.Context ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientPacketHandlers::handleOpenConfigEditor);
    }
}
