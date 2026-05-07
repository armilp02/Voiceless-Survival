package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.events.MobGoalInjector;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateConfigPacket(
        ConfigType configType,
        String targetId,
        boolean enabled,
        double value1,
        double value2,
        double value3,
        boolean boolValue
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UpdateConfigPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EZVCSurvival.MOD_ID, "update_config"));

    // StreamCodec personalizado porque tenemos más de 6 campos
    public static final StreamCodec<ByteBuf, UpdateConfigPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ByteBuf buf, UpdateConfigPacket packet) {
            ByteBufCodecs.VAR_INT.encode(buf, packet.configType.getId());
            ByteBufCodecs.STRING_UTF8.encode(buf, packet.targetId);
            ByteBufCodecs.BOOL.encode(buf, packet.enabled);
            ByteBufCodecs.DOUBLE.encode(buf, packet.value1);
            ByteBufCodecs.DOUBLE.encode(buf, packet.value2);
            ByteBufCodecs.DOUBLE.encode(buf, packet.value3);
            ByteBufCodecs.BOOL.encode(buf, packet.boolValue);
        }

        @Override
        public UpdateConfigPacket decode(ByteBuf buf) {
            ConfigType type = ConfigType.fromId(ByteBufCodecs.VAR_INT.decode(buf));
            String targetId = ByteBufCodecs.STRING_UTF8.decode(buf);
            boolean enabled = ByteBufCodecs.BOOL.decode(buf);
            double value1 = ByteBufCodecs.DOUBLE.decode(buf);
            double value2 = ByteBufCodecs.DOUBLE.decode(buf);
            double value3 = ByteBufCodecs.DOUBLE.decode(buf);
            boolean boolValue = ByteBufCodecs.BOOL.decode(buf);
            return new UpdateConfigPacket(type, targetId, enabled, value1, value2, value3, boolValue);
        }
    };

    public enum ConfigType {
        ENTITY_VOICE(0),
        GENERAL_SOUND(1),
        GENERAL_SOUND_ENTITY(4),
        SOUND_PRIORITY(5);

        private final int id;

        ConfigType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static ConfigType fromId(int id) {
            for (ConfigType type : values()) {
                if (type.id == id) return type;
            }
            return ENTITY_VOICE;
        }
    }

    // Constructor para ENTITY_VOICE
    public UpdateConfigPacket(String entityId, boolean enabled, double speed, double range, double threshold) {
        this(ConfigType.ENTITY_VOICE, entityId, enabled, speed, range, threshold, false);
    }

    // Constructor para GENERAL_SOUND y GENERAL_SOUND_ENTITY
    public UpdateConfigPacket(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier) {
        this(type, soundId, enabled, speedMultiplier, rangeMultiplier, 0.0, false);
    }

    // Constructor para SOUND_PRIORITY
    public UpdateConfigPacket(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier, boolean isPriority) {
        this(type, soundId, enabled, speedMultiplier, rangeMultiplier, 0.0, isPriority);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateConfigPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            boolean configChanged = false;

            if ("global".equals(msg.targetId)) {
                handleGlobalConfigChange(msg);
                configChanged = true;
            } else if ("refresh".equals(msg.targetId)) {
                handleRefreshRequest(msg);
                configChanged = true;
            } else {
                switch (msg.configType) {
                    case ENTITY_VOICE:
                        handleEntityVoiceConfig(msg);
                        configChanged = true;
                        break;
                    case GENERAL_SOUND:
                        handleGeneralSoundConfig(msg);
                        configChanged = true;
                        break;
                    case GENERAL_SOUND_ENTITY:
                        handleGeneralSoundEntityConfig(msg);
                        configChanged = true;
                        break;
                    case SOUND_PRIORITY:
                        handleSoundPriorityConfig(msg);
                        configChanged = true;
                        break;
                }
            }

            if (configChanged) {
                try {
                    switch (msg.configType) {
                        case ENTITY_VOICE:
                            EntityVoiceConfig.init();
                            break;
                        case GENERAL_SOUND:
                        case GENERAL_SOUND_ENTITY:
                        case SOUND_PRIORITY:
                            GeneralSoundsConfig.init();
                            break;
                    }
                    SoundConfig.loadConfigs();
                    if (VoiceConfig.DEBUG.get()) {
                        System.out.println("[EZVCSurvival] Config reload post update: " + msg.configType + " - " + msg.targetId);
                    }
                } catch (Exception e) {
                    if (VoiceConfig.DEBUG.get()) {
                        System.err.println("[EZVCSurvival] Error reloading configs post update: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                MobGoalInjector.refreshAll();
                if (msg.configType == ConfigType.ENTITY_VOICE ||
                        msg.configType == ConfigType.GENERAL_SOUND_ENTITY ||
                        msg.configType == ConfigType.SOUND_PRIORITY) {
                    MobGoalInjector.refreshEntityId(msg.targetId);
                }
            }
        });
    }

    private static void handleEntityVoiceConfig(UpdateConfigPacket msg) {
        EntityVoiceConfig.EntityConfig newConfig =
                new EntityVoiceConfig.EntityConfig(msg.enabled, msg.value1, msg.value2, msg.value3);
        EntityVoiceConfig.set(msg.targetId, newConfig);
        EntityVoiceConfig.persist();
        EntityVoiceConfig.EntityConfig updated = EntityVoiceConfig.get(msg.targetId);
        boolean ok = updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed - msg.value1) < 0.001 &&
                Math.abs(updated.range - msg.value2) < 0.001 &&
                Math.abs(updated.threshold - msg.value3) < 0.001;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] EntityVoice update " + msg.targetId + " ok=" + ok);
        }
    }

    private static void handleGeneralSoundConfig(UpdateConfigPacket msg) {
        if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        if (GeneralSoundsConfig.ROOT.sounds == null) GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<>();
        GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled, msg.value1, msg.value2, msg.boolValue);
        GeneralSoundsConfig.persist();
        GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(msg.targetId);
        boolean ok = updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed_multiplier - msg.value1) < 0.001 &&
                Math.abs(updated.range_multiplier - msg.value2) < 0.001 &&
                updated.is_priority == msg.boolValue;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] GeneralSound update " + msg.targetId + " ok=" + ok);
        }
    }

    private static void handleGeneralSoundEntityConfig(UpdateConfigPacket msg) {
        if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        if (GeneralSoundsConfig.ROOT.mobs == null) GeneralSoundsConfig.ROOT.mobs = new java.util.HashMap<>();
        GeneralSoundsConfig.setMobReaction(msg.targetId, msg.enabled, msg.value1, msg.value2);
        GeneralSoundsConfig.persist();
        GeneralSoundsConfig.Reaction updated = GeneralSoundsConfig.getMobReactions().get(msg.targetId);
        boolean ok = updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed - msg.value1) < 0.001 &&
                Math.abs(updated.range - msg.value2) < 0.001;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] GeneralSoundEntity update " + msg.targetId + " ok=" + ok);
        }
    }

    private static void handleSoundPriorityConfig(UpdateConfigPacket msg) {
        if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        if (GeneralSoundsConfig.ROOT.sounds == null) GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<>();
        GeneralSoundsConfig.SoundEntry se = GeneralSoundsConfig.getSounds().get(msg.targetId);
        if (se != null) {
            se.is_priority = msg.boolValue;
            if (msg.value1 != 0.0) se.speed_multiplier = msg.value1;
            if (msg.value2 != 0.0) se.range_multiplier = msg.value2;
        } else {
            GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled,
                    msg.value1 != 0.0 ? msg.value1 : 1.0,
                    msg.value2 != 0.0 ? msg.value2 : 1.0,
                    msg.boolValue);
        }
        GeneralSoundsConfig.persist();
        GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(msg.targetId);
        boolean ok = updated != null && updated.is_priority == msg.boolValue;
        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] SoundPriority update " + msg.targetId + " ok=" + ok);
        }
    }

    private static void handleGlobalConfigChange(UpdateConfigPacket msg) {
        switch (msg.configType) {
            case ENTITY_VOICE:
                if (EntityVoiceConfig.ROOT == null) EntityVoiceConfig.ROOT = new EntityVoiceConfig.RootConfig();
                EntityVoiceConfig.ROOT.enabled = msg.enabled;
                EntityVoiceConfig.persist();
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Global EntityVoice enabled=" + msg.enabled);
                }
                break;
            case GENERAL_SOUND:
                if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
                GeneralSoundsConfig.ROOT.enabled = msg.enabled;
                GeneralSoundsConfig.persist();
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Global GeneralSound enabled=" + msg.enabled);
                }
                break;
            case SOUND_PRIORITY:
                GeneralSoundsConfig.enableAllPrioritySounds(msg.enabled);
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Global Priority enabled=" + msg.enabled);
                }
                break;
        }
    }

    private static void handleRefreshRequest(UpdateConfigPacket msg) {
        try {
            GeneralSoundsConfig.init();
            EntityVoiceConfig.init();
            SoundConfig.loadConfigs();
            MobGoalInjector.refreshAll();
            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Manual refresh");
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error refreshing configs: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}