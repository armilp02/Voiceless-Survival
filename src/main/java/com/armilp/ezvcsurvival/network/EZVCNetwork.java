package com.armilp.ezvcsurvival.network;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class EZVCNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private EZVCNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Register General Sound Packet (bidirectional)
        registrar.playBidirectional(
                GeneralSoundPacket.TYPE,
                GeneralSoundPacket.STREAM_CODEC,
                GeneralSoundPacket::handle
        );

        // Register Open Config Editor Packet (client-bound)
        registrar.playToClient(
                OpenConfigEditorPacket.TYPE,
                OpenConfigEditorPacket.STREAM_CODEC,
                OpenConfigEditorPacket::handle
        );

        // Register Update Config Packet (server-bound)
        registrar.playToServer(
                UpdateConfigPacket.TYPE,
                UpdateConfigPacket.STREAM_CODEC,
                UpdateConfigPacket::handle
        );
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("ezvcsurvival", path);
    }
}