//package com.armilp.ezvcsurvival.voicechat;
//
//import com.armilp.ezvcsurvival.util.IVoiceChatAdapter;
//import de.maxhenkel.voicechat.api.*;
//import de.maxhenkel.voicechat.api.events.EventRegistration;
//import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
//import de.maxhenkel.voicechat.api.opus.OpusDecoder;
//import net.minecraft.core.BlockPos;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.phys.Vec3;
//
//import javax.annotation.Nullable;
//
//@ForgeVoicechatPlugin
//public class SimpleVoiceChatAdapter implements IVoiceChatAdapter, VoicechatPlugin {
//
//    private static final double VOICE_ACTIVATION_THRESHOLD = -50.0;
//
//    private static VoicechatApi voicechatApi;
//    private @Nullable OpusDecoder decoder;
//    private boolean initialized = false;
//
//    @Override
//    public String getPluginId() {
//        return "ezvcsurvival";
//    }
//
//    @Override
//    public void initialize(VoicechatApi api) {
//        voicechatApi = api;
//        this.initialized = true;
//        System.out.println("[Voiceless Survival] Simple Voice Chat plugin initialized");
//    }
//
//    @Override
//    public void registerEvents(EventRegistration registration) {
//        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
//    }
//
//    @Override
//    public boolean isActive() {
//        return initialized && voicechatApi != null;
//    }
//
//    @Override
//    public void processVoicePacket(ServerPlayer player, short[] audioData, double audioLevel, BlockPos position, boolean isWhispering) {
//        UnifiedVoicePlugin.processVoicePacket(player, audioData, audioLevel, position, isWhispering);
//    }
//
//    private void onMicrophonePacket(MicrophonePacketEvent event) {
//        VoicechatConnection sender = event.getSenderConnection();
//        if (sender == null || sender.getPlayer() == null) return;
//
//        if (!(sender.getPlayer().getPlayer() instanceof ServerPlayer player)) return;
//        if (player.isCreative() || player.isSpectator()) return;
//
//        OpusDecoder localDecoder = decoder;
//        if (localDecoder == null || localDecoder.isClosed()) {
//            localDecoder = voicechatApi.createDecoder();
//            decoder = localDecoder;
//        }
//        if (localDecoder == null) return;
//
//        localDecoder.resetState();
//
//        byte[] opusEncodedData = event.getPacket().getOpusEncodedData();
//        short[] decoded;
//        try {
//            decoded = localDecoder.decode(opusEncodedData);
//        } catch (Exception e) {
//            return;
//        }
//
//        if (decoded == null || decoded.length == 0) return;
//
//        double audioLevel = UnifiedVoicePlugin.getMaxAudioLevel(decoded);
//
//        if (audioLevel < VOICE_ACTIVATION_THRESHOLD) {
//            return;
//        }
//
//        Position voicechatPosition = sender.getPlayer().getPosition();
//        Vec3 senderVec = new Vec3(
//                voicechatPosition.getX(),
//                voicechatPosition.getY(),
//                voicechatPosition.getZ()
//        );
//
//        BlockPos playerPosition = new BlockPos(
//                (int) Math.floor(senderVec.x),
//                (int) Math.floor(senderVec.y),
//                (int) Math.floor(senderVec.z)
//        );
//
//        boolean isWhispering = event.getPacket().isWhispering();
//
//        processVoicePacket(player, decoded, audioLevel, playerPosition, isWhispering);
//    }
//}