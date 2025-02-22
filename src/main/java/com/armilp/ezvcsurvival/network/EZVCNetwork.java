package com.armilp.ezvcsurvival.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class EZVCNetwork {
    public static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("ezvcsurvival", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static int nextID() {
        return packetId++;
    }

    public static void registerPackets() {
        INSTANCE.messageBuilder(SoundPlayedPacket.class, nextID())
                .encoder(SoundPlayedPacket::encode)
                .decoder(SoundPlayedPacket::decode)
                .consumerMainThread(SoundPlayedPacket::handle)
                .add();
        INSTANCE.messageBuilder(PointBlankSoundPacket.class, nextID())
                .encoder(PointBlankSoundPacket::encode)
                .decoder(PointBlankSoundPacket::decode)
                .consumerMainThread(PointBlankSoundPacket::handle)
                .add();
    }
}
