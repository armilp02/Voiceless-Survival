package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.voicechat.client.ClientVoiceLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VoiceLevelPacket {

    private final double db;

    public VoiceLevelPacket(double db) {
        this.db = db;
    }

    public static void encode(VoiceLevelPacket packet, FriendlyByteBuf buf) {
        buf.writeDouble(packet.db);
    }

    public static VoiceLevelPacket decode(FriendlyByteBuf buf) {
        return new VoiceLevelPacket(buf.readDouble());
    }

    public static void handle(VoiceLevelPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        ClientVoiceLevel.update(packet.db);
    }
}