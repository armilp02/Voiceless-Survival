//package com.armilp.ezvcsurvival.voicechat;
//
//import com.armilp.ezvcsurvival.util.IVoiceChatAdapter;
//import net.minecraft.core.BlockPos;
//import net.minecraft.server.level.ServerPlayer;
//import su.plo.voice.api.addon.AddonInitializer;
//import su.plo.voice.api.addon.InjectPlasmoVoice;
//import su.plo.voice.api.addon.annotation.Addon;
//import su.plo.voice.api.audio.codec.AudioDecoder;
//import su.plo.voice.api.audio.codec.CodecException;
//import su.plo.voice.api.event.EventPriority;
//import su.plo.voice.api.event.EventSubscribe;
//import su.plo.voice.api.server.PlasmoVoiceServer;
//import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
//import su.plo.voice.api.server.player.VoicePlayer;
//import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;
//
//import javax.annotation.Nullable;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Addon(
//        id = "ezvcsurvival_plasmo",
//        name = "Voiceless Survival",
//        version = "2.1.0",
//        authors = {"armilp"}
//)
//public class PlasmoVoiceAdapter implements IVoiceChatAdapter, AddonInitializer {
//
//    private static final double VOICE_ACTIVATION_THRESHOLD = -50.0;
//
//    @InjectPlasmoVoice
//    private PlasmoVoiceServer voiceServer;
//
//    private final Map<UUID, AudioDecoder> decoderMap = new ConcurrentHashMap<>();
//    private boolean initialized = false;
//
//    @Override
//    public void onAddonInitialize() {
//        if (voiceServer == null) {
//            System.err.println("[PlasmoVoiceAdapter] Voice server not found");
//            return;
//        }
//
//        try {
//            voiceServer.getEventBus().register(this, this);
//            initialized = true;
//            System.out.println("[PlasmoVoiceAdapter] Initialized successfully");
//        } catch (Exception e) {
//            System.err.println("[PlasmoVoiceAdapter] Init error: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public void onAddonShutdown() {
//        decoderMap.values().forEach(decoder -> {
//            if (decoder != null) {
//                decoder.close();
//            }
//        });
//        decoderMap.clear();
//
//        if (voiceServer != null) {
//            voiceServer.getEventBus().unregister(this, this);
//        }
//        initialized = false;
//    }
//
//    @Override
//    public String getPluginId() {
//        return "ezvcsurvival_plasmo";
//    }
//
//    @Override
//    public boolean isActive() {
//        return initialized && voiceServer != null;
//    }
//
//    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
//    public void onPlayerSpeak(PlayerSpeakEvent event) {
//        VoicePlayer voicePlayer = event.getPlayer();
//        Object playerInstance = voicePlayer.getInstance().getInstance();
//
//        if (!(playerInstance instanceof ServerPlayer serverPlayer)) {
//            return;
//        }
//
//        if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
//            return;
//        }
//
//        PlayerAudioPacket packet = event.getPacket();
//        UUID playerId = serverPlayer.getUUID();
//
//        AudioDecoder decoder = decoderMap.get(playerId);
//        if (decoder == null || !decoder.isOpen()) {
//            decoder = createDecoder(packet.isStereo());
//            if (decoder == null) {
//                return;
//            }
//            decoderMap.put(playerId, decoder);
//        }
//
//        try {
//            decoder.reset();
//        } catch (Exception ignored) {}
//
//        short[] decodedAudio;
//        try {
//            decodedAudio = decoder.decode(packet.getData());
//        } catch (CodecException e) {
//            return;
//        }
//
//        if (decodedAudio == null || decodedAudio.length == 0) {
//            return;
//        }
//
//        double audioLevel = UnifiedVoicePlugin.getMaxAudioLevel(decodedAudio);
//
//        if (audioLevel < VOICE_ACTIVATION_THRESHOLD) {
//            return;
//        }
//
//        BlockPos position = new BlockPos(
//                (int) Math.floor(serverPlayer.getX()),
//                (int) Math.floor(serverPlayer.getY()),
//                (int) Math.floor(serverPlayer.getZ())
//        );
//
//        boolean isWhispering = packet.getDistance() > 0 && packet.getDistance() < 16;
//
//        processVoicePacket(serverPlayer, decodedAudio, audioLevel, position, isWhispering);
//    }
//
//    @Override
//    public void processVoicePacket(ServerPlayer player, short[] audioData, double audioLevel, BlockPos position, boolean isWhispering) {
//        UnifiedVoicePlugin.processVoicePacket(player, audioData, audioLevel, position, isWhispering);
//    }
//
//    @Nullable
//    private AudioDecoder createDecoder(boolean isStereo) {
//        try {
//            AudioDecoder decoder = voiceServer.createOpusDecoder(isStereo);
//            decoder.open();
//            return decoder;
//        } catch (Exception e) {
//            return null;
//        }
//    }
//}