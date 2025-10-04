package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.events.SoundEventTracker;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GeneralSoundPacket(
        ResourceLocation sound,
        double x,
        double y,
        double z,
        double speedMultiplier,
        double rangeMultiplier
) implements CustomPacketPayload {

    public static final Type<GeneralSoundPacket> TYPE = new Type<>(EZVCNetwork.id("general_sound"));

    public static final StreamCodec<ByteBuf, GeneralSoundPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, GeneralSoundPacket::sound,
            ByteBufCodecs.DOUBLE, GeneralSoundPacket::x,
            ByteBufCodecs.DOUBLE, GeneralSoundPacket::y,
            ByteBufCodecs.DOUBLE, GeneralSoundPacket::z,
            ByteBufCodecs.DOUBLE, GeneralSoundPacket::speedMultiplier,
            ByteBufCodecs.DOUBLE, GeneralSoundPacket::rangeMultiplier,
            GeneralSoundPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GeneralSoundPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            SoundEventTracker.setLastPlayedPosition(
                    packet.sound,
                    packet.x,
                    packet.y,
                    packet.z,
                    packet.speedMultiplier,
                    packet.rangeMultiplier
            );
        });
    }
}