package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.commands.SoundEffectCommand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class SoundPlayedPacket {
    private final String soundName;

    public SoundPlayedPacket(String soundName) {
        this.soundName = soundName;
    }

    public static void encode(SoundPlayedPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.soundName);
    }

    public static SoundPlayedPacket decode(FriendlyByteBuf buf) {
        return new SoundPlayedPacket(buf.readUtf());
    }

    public static void handle(SoundPlayedPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                SoundEffectCommand.applyEffect(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
