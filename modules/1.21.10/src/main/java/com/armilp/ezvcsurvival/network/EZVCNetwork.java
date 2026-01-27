package com.armilp.ezvcsurvival.network;

import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public final class EZVCNetwork {

    public static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel INSTANCE = ChannelBuilder
            .named("ezvcsurvival:main")
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions((status, version) ->
                    status == Channel.VersionTest.Status.VANILLA ||
                            (status == Channel.VersionTest.Status.PRESENT && version == PROTOCOL_VERSION))
            .serverAcceptedVersions((status, version) ->
                    status == Channel.VersionTest.Status.VANILLA ||
                            (status == Channel.VersionTest.Status.PRESENT && version == PROTOCOL_VERSION))
            .simpleChannel();

    private static int packetId = 0;

    private EZVCNetwork() {
    }

    public static int nextID() {
        return packetId++;
    }

    public static void registerPackets() {
        INSTANCE.messageBuilder(GeneralSoundPacket.class, nextID())
                .encoder(GeneralSoundPacket::encode)
                .decoder(GeneralSoundPacket::decode)
                .consumerMainThread(GeneralSoundPacket::handle)
                .add();

        INSTANCE.messageBuilder(OpenConfigEditorPacket.class, nextID())
                .encoder(OpenConfigEditorPacket::encode)
                .decoder(OpenConfigEditorPacket::decode)
                .consumerMainThread(OpenConfigEditorPacket::handle)
                .add();

        INSTANCE.messageBuilder(UpdateConfigPacket.class, nextID())
                .encoder(UpdateConfigPacket::encode)
                .decoder(UpdateConfigPacket::decode)
                .consumerMainThread(UpdateConfigPacket::handle)
                .add();

        // Opcional: bloquea la definición del canal para evitar cambios posteriores.
        INSTANCE.build();
    }
}