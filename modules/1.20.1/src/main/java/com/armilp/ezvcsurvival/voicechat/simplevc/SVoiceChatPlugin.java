package com.armilp.ezvcsurvival.voicechat.simplevc;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.voicechat.VoiceProcessor;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

@ForgeVoicechatPlugin
public class SVoiceChatPlugin implements VoicechatPlugin {

    private static VoicechatApi voicechatApi;

    @Nullable
    private OpusDecoder decoder;

    @Override
    public String getPluginId() {
        return "ezvcsurvival";
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[DEBUG] VoiceChat Plugin initialized");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[DEBUG] Registered MicrophonePacketEvent");
        }
    }

    public void onMicrophonePacket(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.getPlayer() == null) return;

        if (!(sender.getPlayer().getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        OpusDecoder localDecoder = decoder;
        if (localDecoder == null || localDecoder.isClosed()) {
            localDecoder = voicechatApi.createDecoder();
            decoder = localDecoder;
        }
        if (localDecoder == null) {
            return;
        }
        localDecoder.resetState();

        byte[] opusEncodedData = event.getPacket().getOpusEncodedData();
        short[] decoded;
        try {
            decoded = localDecoder.decode(opusEncodedData);
        } catch (Exception e) {
            return;
        }

        double audioLevel = VoiceProcessor.getMaxAudioLevel(decoded);

        Position voicechatPosition = sender.getPlayer().getPosition();
        Vec3 senderVec = new Vec3(
                voicechatPosition.getX(),
                voicechatPosition.getY(),
                voicechatPosition.getZ()
        );

        VoiceProcessor.processAudio(player, senderVec, audioLevel, event.getPacket().isWhispering());
    }
}