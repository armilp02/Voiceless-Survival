package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.util.IGoalRefresher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

public class UpdateConfigPacket {

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

    private final ConfigType configType;
    private final String targetId;
    private final boolean enabled;
    private final double value1;
    private final double value2;
    private final double value3;
    private final boolean boolValue;

    public UpdateConfigPacket(String entityId, boolean enabled, double speed, double range, double threshold) {
        this.configType = ConfigType.ENTITY_VOICE;
        this.targetId = entityId;
        this.enabled = enabled;
        this.value1 = speed;
        this.value2 = range;
        this.value3 = threshold;
        this.boolValue = false;
    }

    public UpdateConfigPacket(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier) {
        this.configType = type;
        this.targetId = soundId;
        this.enabled = enabled;
        this.value1 = speedMultiplier;
        this.value2 = rangeMultiplier;
        this.value3 = 0.0;
        this.boolValue = false;
    }

    public UpdateConfigPacket(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier, boolean isPriority) {
        this.configType = type;
        this.targetId = soundId;
        this.enabled = enabled;
        this.value1 = speedMultiplier;
        this.value2 = rangeMultiplier;
        this.value3 = 0.0;
        this.boolValue = isPriority;
    }

    public static void encode(UpdateConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.configType.getId());
        buf.writeUtf(msg.targetId);
        buf.writeBoolean(msg.enabled);
        buf.writeDouble(msg.value1);
        buf.writeDouble(msg.value2);
        buf.writeDouble(msg.value3);
        buf.writeBoolean(msg.boolValue);
    }

    public static UpdateConfigPacket decode(FriendlyByteBuf buf) {
        ConfigType type = ConfigType.fromId(buf.readInt());
        String targetId = buf.readUtf();
        boolean enabled = buf.readBoolean();
        double value1 = buf.readDouble();
        double value2 = buf.readDouble();
        double value3 = buf.readDouble();
        boolean boolValue = buf.readBoolean();

        switch (type) {
            case ENTITY_VOICE:
                return new UpdateConfigPacket(targetId, enabled, value1, value2, value3);
            case GENERAL_SOUND:
                return new UpdateConfigPacket(ConfigType.GENERAL_SOUND, targetId, enabled, value1, value2, boolValue);
            case GENERAL_SOUND_ENTITY:
                return new UpdateConfigPacket(ConfigType.GENERAL_SOUND_ENTITY, targetId, enabled, value1, value2);
            case SOUND_PRIORITY:
                return new UpdateConfigPacket(ConfigType.SOUND_PRIORITY, targetId, enabled, value1, value2, boolValue);
            default:
                return new UpdateConfigPacket(targetId, enabled, value1, value2, value3);
        }
    }

    public static void handle(UpdateConfigPacket msg, CustomPayloadEvent.Context ctx) {
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
                    System.out.println("[EZVCSurvival] Configurations reloaded successfully after update: " + msg.configType + " - " + msg.targetId);
                }
            } catch (Exception e) {
                if (VoiceConfig.DEBUG.get()) {
                    System.err.println("[EZVCSurvival] Error reloading configurations after update: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            refreshAllEntityGoals();

            if (msg.configType == ConfigType.ENTITY_VOICE ||
                    msg.configType == ConfigType.GENERAL_SOUND_ENTITY ||
                    msg.configType == ConfigType.SOUND_PRIORITY) {
                refreshSpecificEntityGoals(msg.targetId);
            }
        }
        // consumerMainThread gestiona enqueueWork y setPacketHandled.
    }

    private static void handleEntityVoiceConfig(UpdateConfigPacket msg) {
        EntityVoiceConfig.EntityConfig newConfig =
                new EntityVoiceConfig.EntityConfig(msg.enabled, msg.value1, msg.value2, msg.value3);
        EntityVoiceConfig.set(msg.targetId, newConfig);
        EntityVoiceConfig.persist();

        EntityVoiceConfig.EntityConfig updated = EntityVoiceConfig.get(msg.targetId);
        boolean verificationPassed = (updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed - msg.value1) < 0.001 &&
                Math.abs(updated.range - msg.value2) < 0.001 &&
                Math.abs(updated.threshold - msg.value3) < 0.001);

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Server updated EntityVoice config for: " + msg.targetId +
                    " enabled=" + msg.enabled + " speed=" + msg.value1 + " range=" + msg.value2 +
                    " threshold=" + msg.value3 + " verification=" + verificationPassed);
        }
    }

    private static void handleGeneralSoundConfig(UpdateConfigPacket msg) {
        if (GeneralSoundsConfig.ROOT == null) {
            GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        }
        if (GeneralSoundsConfig.ROOT.sounds == null) {
            GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<>();
        }

        GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled, msg.value1, msg.value2, msg.boolValue);
        GeneralSoundsConfig.persist();

        GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(msg.targetId);
        boolean verificationPassed = (updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed_multiplier - msg.value1) < 0.001 &&
                Math.abs(updated.range_multiplier - msg.value2) < 0.001 &&
                updated.is_priority == msg.boolValue);

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Server updated GeneralSound config for: " + msg.targetId +
                    " enabled=" + msg.enabled + " speed=" + msg.value1 + " range=" + msg.value2 +
                    " isPriority=" + msg.boolValue + " verification=" + verificationPassed);
        }
    }

    private static void handleGeneralSoundEntityConfig(UpdateConfigPacket msg) {
        if (GeneralSoundsConfig.ROOT == null) {
            GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        }
        if (GeneralSoundsConfig.ROOT.mobs == null) {
            GeneralSoundsConfig.ROOT.mobs = new java.util.HashMap<>();
        }

        GeneralSoundsConfig.setMobReaction(msg.targetId, msg.enabled, msg.value1, msg.value2);
        GeneralSoundsConfig.persist();

        GeneralSoundsConfig.Reaction updated = GeneralSoundsConfig.getMobReactions().get(msg.targetId);
        boolean verificationPassed = (updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed - msg.value1) < 0.001 &&
                Math.abs(updated.range - msg.value2) < 0.001);

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Server updated GeneralSound entity config for: " + msg.targetId +
                    " enabled=" + msg.enabled + " speed=" + msg.value1 + " range=" + msg.value2 +
                    " verification=" + verificationPassed);
        }
    }

    private static void handleSoundPriorityConfig(UpdateConfigPacket msg) {
        if (GeneralSoundsConfig.ROOT == null) {
            GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        }
        if (GeneralSoundsConfig.ROOT.sounds == null) {
            GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<>();
        }

        GeneralSoundsConfig.SoundEntry soundEntry = GeneralSoundsConfig.getSounds().get(msg.targetId);
        if (soundEntry != null) {
            soundEntry.is_priority = msg.boolValue;
            if (msg.value1 != 0.0) {
                soundEntry.speed_multiplier = msg.value1;
            }
            if (msg.value2 != 0.0) {
                soundEntry.range_multiplier = msg.value2;
            }
        } else {
            GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled,
                    msg.value1 != 0.0 ? msg.value1 : 1.0,
                    msg.value2 != 0.0 ? msg.value2 : 1.0,
                    msg.boolValue);
        }

        GeneralSoundsConfig.persist();

        GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(msg.targetId);
        boolean verificationPassed = (updated != null &&
                updated.is_priority == msg.boolValue &&
                Math.abs(updated.speed_multiplier - (msg.value1 != 0.0 ? msg.value1 : updated.speed_multiplier)) < 0.001 &&
                Math.abs(updated.range_multiplier - (msg.value2 != 0.0 ? msg.value2 : updated.range_multiplier)) < 0.001);

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Server updated Sound Priority for: " + msg.targetId +
                    " isPriority=" + msg.boolValue + " speed=" + msg.value1 + " range=" + msg.value2 +
                    " verification=" + verificationPassed);
        }
    }

    private static void handleGlobalConfigChange(UpdateConfigPacket msg) {
        switch (msg.configType) {
            case ENTITY_VOICE:
                if (EntityVoiceConfig.ROOT == null) {
                    EntityVoiceConfig.ROOT = new EntityVoiceConfig.RootConfig();
                }
                EntityVoiceConfig.ROOT.enabled = msg.enabled;
                EntityVoiceConfig.persist();

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Server updated EntityVoice global config: enabled=" + msg.enabled);
                }
                break;
            case GENERAL_SOUND:
                if (GeneralSoundsConfig.ROOT == null) {
                    GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
                }
                GeneralSoundsConfig.ROOT.enabled = msg.enabled;
                GeneralSoundsConfig.persist();

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Server updated GeneralSound global config: enabled=" + msg.enabled);
                }
                break;
            case SOUND_PRIORITY:
                GeneralSoundsConfig.enableAllPrioritySounds(msg.enabled);

                if (VoiceConfig.DEBUG.get()) {
                    System.out.println("[EZVCSurvival] Server updated Sound Priority global config: enabled=" + msg.enabled);
                }
                break;
        }
    }

    private static void handleRefreshRequest(UpdateConfigPacket msg) {
        try {
            GeneralSoundsConfig.init();
            EntityVoiceConfig.init();
            SoundConfig.loadConfigs();
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error refreshing configs: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void refreshAllEntityGoals() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return;
        }

        try {
            for (ServerLevel level : server.getAllLevels()) {
                Iterable<Entity> allEntities = level.getAllEntities();
                for (Entity e : allEntities) {
                    if (!(e instanceof Mob mob)) continue;

                    if (mob instanceof IGoalRefresher refresher) {
                        try {
                            refresher.ezvcsurvival$RefreshGoals();
                        } catch (Exception ex) {
                            if (VoiceConfig.DEBUG.get()) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (NoSuchMethodError nsme) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] update packet: level.getAllEntities() does not exist in this mapping.");
                nsme.printStackTrace();
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error to refresh goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void refreshSpecificEntityGoals(String entityId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return;
        }

        try {
            for (ServerLevel level : server.getAllLevels()) {
                Iterable<Entity> allEntities = level.getAllEntities();
                for (Entity e : allEntities) {
                    if (!(e instanceof Mob mob)) continue;

                    ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
                    if (mobId == null) continue;

                    if (mobId.toString().equals(entityId)) {
                        if (mob instanceof IGoalRefresher refresher) {
                            try {
                                refresher.ezvcsurvival$RefreshGoals();
                            } catch (Exception ex) {
                                if (VoiceConfig.DEBUG.get()) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        } catch (NoSuchMethodError nsme) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] update packet: level.getAllEntities() does not exist in this mapping.");
                nsme.printStackTrace();
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error to refresh goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}