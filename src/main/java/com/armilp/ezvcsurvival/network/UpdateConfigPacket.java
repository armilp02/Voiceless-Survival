package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.events.MobGoalInjector;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

    public static final Type<UpdateConfigPacket> TYPE = new Type<>(EZVCNetwork.id("update_config"));

    public static final StreamCodec<ByteBuf, UpdateConfigPacket> STREAM_CODEC = new StreamCodec<ByteBuf, UpdateConfigPacket>() {
        @Override
        public UpdateConfigPacket decode(ByteBuf buffer) {
            ConfigType configType = ConfigType.fromId(ByteBufCodecs.VAR_INT.decode(buffer));
            String targetId = ByteBufCodecs.STRING_UTF8.decode(buffer);
            boolean enabled = ByteBufCodecs.BOOL.decode(buffer);
            double value1 = ByteBufCodecs.DOUBLE.decode(buffer);
            double value2 = ByteBufCodecs.DOUBLE.decode(buffer);
            double value3 = ByteBufCodecs.DOUBLE.decode(buffer);
            boolean boolValue = ByteBufCodecs.BOOL.decode(buffer);

            return new UpdateConfigPacket(configType, targetId, enabled, value1, value2, value3, boolValue);
        }

        @Override
        public void encode(ByteBuf buffer, UpdateConfigPacket packet) {
            ByteBufCodecs.VAR_INT.encode(buffer, packet.configType().getId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, packet.targetId());
            ByteBufCodecs.BOOL.encode(buffer, packet.enabled());
            ByteBufCodecs.DOUBLE.encode(buffer, packet.value1());
            ByteBufCodecs.DOUBLE.encode(buffer, packet.value2());
            ByteBufCodecs.DOUBLE.encode(buffer, packet.value3());
            ByteBufCodecs.BOOL.encode(buffer, packet.boolValue());
        }
    };

    // Constructores de conveniencia - Fixed to match the primary constructor
    public UpdateConfigPacket(String entityId, boolean enabled, double speed, double range, double threshold) {
        this(ConfigType.ENTITY_VOICE, entityId, enabled, speed, range, threshold, false);
    }

    public UpdateConfigPacket(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier) {
        this(type, soundId, enabled, speedMultiplier, rangeMultiplier, 0.0, false);
    }

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
                    case ENTITY_VOICE -> {
                        handleEntityVoiceConfig(msg);
                        configChanged = true;
                    }
                    case GENERAL_SOUND -> {
                        handleGeneralSoundConfig(msg);
                        configChanged = true;
                    }
                    case GENERAL_SOUND_ENTITY -> {
                        handleGeneralSoundEntityConfig(msg);
                        configChanged = true;
                    }
                    case SOUND_PRIORITY -> {
                        handleSoundPriorityConfig(msg);
                        configChanged = true;
                    }
                }
            }

            if (configChanged) {
                try {
                    switch (msg.configType) {
                        case ENTITY_VOICE -> EntityVoiceConfig.init();
                        case GENERAL_SOUND, GENERAL_SOUND_ENTITY, SOUND_PRIORITY -> GeneralSoundsConfig.init();
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

    public enum ConfigType {
        ENTITY_VOICE(0),
        GENERAL_SOUND(1),
        GENERAL_SOUND_ENTITY(4),
        SOUND_PRIORITY(5);

        private final int id;
        ConfigType(int id) { this.id = id; }
        public int getId() { return id; }
        public static ConfigType fromId(int id) {
            for (ConfigType type : values()) if (type.id == id) return type;
            return ENTITY_VOICE;
        }
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
            case ENTITY_VOICE -> {
                if (EntityVoiceConfig.ROOT == null) EntityVoiceConfig.ROOT = new EntityVoiceConfig.RootConfig();
                EntityVoiceConfig.ROOT.enabled = msg.enabled;
                EntityVoiceConfig.persist();
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Global EntityVoice enabled=" + msg.enabled);
                }
            }
            case GENERAL_SOUND -> {
                if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
                GeneralSoundsConfig.ROOT.enabled = msg.enabled;
                GeneralSoundsConfig.persist();
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Global GeneralSound enabled=" + msg.enabled);
                }
            }
            case SOUND_PRIORITY -> {
                GeneralSoundsConfig.enableAllPrioritySounds(msg.enabled);
                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Global Priority enabled=" + msg.enabled);
                }
            }
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