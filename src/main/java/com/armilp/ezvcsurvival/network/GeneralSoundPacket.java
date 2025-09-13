package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.events.SoundEventTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GeneralSoundPacket {
    private final ResourceLocation sound;
    private final double x;
    private final double y;
    private final double z;
    private final double speedMultiplier;
    private final double rangeMultiplier;

    public GeneralSoundPacket(ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        this.sound = sound;
        this.x = x;
        this.y = y;
        this.z = z;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
    }

    public static void encode(GeneralSoundPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.sound);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeDouble(packet.speedMultiplier);
        buf.writeDouble(packet.rangeMultiplier);
    }

    public static GeneralSoundPacket decode(FriendlyByteBuf buf) {
        ResourceLocation sound = buf.readResourceLocation();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        double speedMultiplier = buf.readDouble();
        double rangeMultiplier = buf.readDouble();
        return new GeneralSoundPacket(sound, x, y, z, speedMultiplier, rangeMultiplier);
    }

    public static void handle(GeneralSoundPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> SoundEventTracker.setLastPlayedPosition(
                packet.sound,
                packet.x,
                packet.y,
                packet.z,
                packet.speedMultiplier,
                packet.rangeMultiplier
        ));
        ctx.get().setPacketHandled(true);
    }
}


