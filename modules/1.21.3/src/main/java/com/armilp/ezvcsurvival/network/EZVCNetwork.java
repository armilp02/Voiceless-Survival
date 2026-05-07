package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = EZVCSurvival.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class EZVCNetwork {

    private EZVCNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // Registrar paquete de sonido general (bidireccional)
        registrar.playBidirectional(
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