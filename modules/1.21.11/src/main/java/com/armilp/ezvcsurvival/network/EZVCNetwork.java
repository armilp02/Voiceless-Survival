package com.armilp.ezvcsurvival.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class EZVCNetwork {

    private EZVCNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // Registrar paquete de sonido general (bidireccional)
        registrar.playToServer(
                GeneralSoundPacket.TYPE,
                GeneralSoundPacket.STREAM_CODEC,
                GeneralSoundPacket::handle
        );

        // Registrar paquete de abrir config (servidor -> cliente)
        registrar.playToClient(
                OpenConfigEditorPacket.TYPE,
                OpenConfigEditorPacket.STREAM_CODEC,
                OpenConfigEditorPacket::handle
        );

        // Registrar paquete de actualizar config (cliente -> servidor)
        registrar.playToServer(
                UpdateConfigPacket.TYPE,
                UpdateConfigPacket.STREAM_CODEC,
                UpdateConfigPacket::handle
        );
    }
}