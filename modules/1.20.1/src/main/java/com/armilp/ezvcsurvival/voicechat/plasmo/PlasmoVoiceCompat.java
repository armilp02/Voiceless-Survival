package com.armilp.ezvcsurvival.voicechat.plasmo;

import com.armilp.ezvcsurvival.voicechat.VoiceProcessor;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.audio.codec.AudioDecoder;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Addon(
        id = "pv-addon-ezvcsurvival",
        name = "plasmo-ezvcsurvival",
        version = "2.2.0",
        authors = {"armilp", "skynetcloud"}
)
public final class PlasmoVoiceCompat implements AddonInitializer {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROXIMITY_ACTIVATION = "proximity";

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;

    private final Map<UUID, DecoderHolder> decoders = new ConcurrentHashMap<>();
    private final AtomicBoolean errorLogged = new AtomicBoolean();
    private volatile UUID proximityId;

    public static void register() {
        PlasmoVoiceCompat addon = new PlasmoVoiceCompat();
        PlasmoVoiceServer.getAddonsLoader().load(addon);

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) ->
                addon.closeDecoder(event.getEntity().getUUID()));
    }

    @Override
    public void onAddonInitialize() {
        ServerActivation activation = voiceServer.getActivationManager()
                .getActivationByName(PROXIMITY_ACTIVATION)
                .orElse(null);

        if (activation == null) {
            LOGGER.error("[ezvcsurvival] Plasmo Voice: '{}' activation not found, integration disabled", PROXIMITY_ACTIVATION);
            return;
        }

        proximityId = activation.getId();

        activation.onPlayerActivationEnd((player, packet) -> {
            closeDecoder(player.getInstance().getUuid());
            return ServerActivation.Result.IGNORED;
        });

        voiceServer.getEventBus().register(this, this);
    }

    @Override
    public void onAddonShutdown() {
        decoders.values().forEach(DecoderHolder::close);
        decoders.clear();
    }

    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerSpeak(PlayerSpeakEvent event) {
        PlayerAudioPacket packet = event.getPacket();
        if (packet.isStereo() || !packet.getActivationId().equals(proximityId)) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        UUID uuid = event.getPlayer().getInstance().getUuid();
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        try {
            byte[] decrypted = voiceServer.getDefaultEncryption().decrypt(packet.getData());
            short[] decoded = getHolder(uuid).decode(decrypted);
            if (decoded == null) {
                return;
            }
            VoiceProcessor.processAudio(player, player.position(), VoiceProcessor.getMaxAudioLevel(decoded), false);
        } catch (Exception e) {
            if (errorLogged.compareAndSet(false, true)) {
                LOGGER.error("[ezvcsurvival] Plasmo Voice: failed to process audio", e);
            }
        }
    }

    private DecoderHolder getHolder(UUID uuid) throws Exception {
        DecoderHolder existing = decoders.get(uuid);
        if (existing != null) {
            return existing;
        }

        AudioDecoder created = voiceServer.createOpusDecoder(false);
        created.open();
        DecoderHolder holder = new DecoderHolder(created);

        DecoderHolder previous = decoders.putIfAbsent(uuid, holder);
        if (previous != null) {
            holder.close();
            return previous;
        }
        return holder;
    }

    private void closeDecoder(UUID uuid) {
        DecoderHolder holder = decoders.remove(uuid);
        if (holder != null) {
            holder.close();
        }
    }
}