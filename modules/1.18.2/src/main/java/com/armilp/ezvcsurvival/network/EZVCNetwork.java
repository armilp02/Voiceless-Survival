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
        INSTANCE.messageBuilder(GeneralSoundPacket.class, nextID())
                .encoder(GeneralSoundPacket::encode)
                .decoder(GeneralSoundPacket::decode)
                .consumer(GeneralSoundPacket::handle)
                .add();
        INSTANCE.messageBuilder(OpenConfigEditorPacket.class, nextID())
                .encoder(OpenConfigEditorPacket::encode)
                .decoder(OpenConfigEditorPacket::decode)
                .consumer(OpenConfigEditorPacket::handle)
                .add();
        INSTANCE.messageBuilder(UpdateConfigPacket.class, nextID())
                .encoder(UpdateConfigPacket::encode)
                .decoder(UpdateConfigPacket::decode)
                .consumer(UpdateConfigPacket::handle)
                .add();
    }
}
