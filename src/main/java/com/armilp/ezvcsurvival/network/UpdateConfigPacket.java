package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.GunfireConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.util.IGoalRefresher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.function.Supplier;

public class UpdateConfigPacket {

    public enum ConfigType {
        ENTITY_VOICE(0),
        GENERAL_SOUND(1),
        GUNFIRE_SOUND(2),
        GUNFIRE_ENTITY(3),
        GENERAL_SOUND_ENTITY(4);

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

    public UpdateConfigPacket(String entityId, boolean enabled, double speed, double range, double threshold) {
        this.configType = ConfigType.ENTITY_VOICE;
        this.targetId = entityId;
        this.enabled = enabled;
        this.value1 = speed;
        this.value2 = range;
        this.value3 = threshold;
    }

    public UpdateConfigPacket(ConfigType type, String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier) {
        this.configType = type;
        this.targetId = soundId;
        this.enabled = enabled;
        this.value1 = speedMultiplier;
        this.value2 = rangeMultiplier;
        this.value3 = 0.0;
    }

    public UpdateConfigPacket(String entityId, boolean enabled, double speed, double range) {
        this.configType = ConfigType.GUNFIRE_ENTITY;
        this.targetId = entityId;
        this.enabled = enabled;
        this.value1 = speed;
        this.value2 = range;
        this.value3 = 0.0;
    }

    public static void encode(UpdateConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.configType.getId());
        buf.writeUtf(msg.targetId);
        buf.writeBoolean(msg.enabled);
        buf.writeDouble(msg.value1);
        buf.writeDouble(msg.value2);
        buf.writeDouble(msg.value3);
    }

    public static UpdateConfigPacket decode(FriendlyByteBuf buf) {
        ConfigType type = ConfigType.fromId(buf.readInt());
        String targetId = buf.readUtf();
        boolean enabled = buf.readBoolean();
        double value1 = buf.readDouble();
        double value2 = buf.readDouble();
        double value3 = buf.readDouble();

        switch (type) {
            case ENTITY_VOICE:
                return new UpdateConfigPacket(targetId, enabled, value1, value2, value3);
            case GENERAL_SOUND:
                return new UpdateConfigPacket(ConfigType.GENERAL_SOUND, targetId, enabled, value1, value2);
            case GUNFIRE_SOUND:
                return new UpdateConfigPacket(ConfigType.GUNFIRE_SOUND, targetId, enabled, value1, value2);
            case GUNFIRE_ENTITY:
                return new UpdateConfigPacket(targetId, enabled, value1, value2);
            case GENERAL_SOUND_ENTITY:
                return new UpdateConfigPacket(ConfigType.GENERAL_SOUND_ENTITY, targetId, enabled, value1, value2);
            default:
                return new UpdateConfigPacket(targetId, enabled, value1, value2, value3);
        }
    }

    public static void handle(UpdateConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if ("global".equals(msg.targetId)) {
                handleGlobalConfigChange(msg);
            } else if ("refresh".equals(msg.targetId)) {
                handleRefreshRequest(msg);
            } else {
                switch (msg.configType) {
                    case ENTITY_VOICE:
                        handleEntityVoiceConfig(msg);
                        break;
                    case GENERAL_SOUND:
                        handleGeneralSoundConfig(msg);
                        break;
                    case GUNFIRE_SOUND:
                        handleGunfireSoundConfig(msg);
                        break;
                    case GUNFIRE_ENTITY:
                        handleGunfireEntityConfig(msg);
                        break;
                    case GENERAL_SOUND_ENTITY:
                        handleGeneralSoundEntityConfig(msg);
                        break;
                }
            }

            try {
                SoundConfig.loadConfigs();
            } catch (Exception e) {
                if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
                    System.err.println("[EZVCSurvival] Error reloading SoundConfig after update: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            refreshAllEntityGoals();

            if (msg.configType == ConfigType.ENTITY_VOICE || 
                msg.configType == ConfigType.GUNFIRE_ENTITY || 
                msg.configType == ConfigType.GENERAL_SOUND_ENTITY || 
                msg.configType == ConfigType.GENERAL_SOUND) {
                refreshSpecificEntityGoals(msg.targetId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static void handleEntityVoiceConfig(UpdateConfigPacket msg) {
        EntityVoiceConfig.EntityConfig newConfig = 
                new EntityVoiceConfig.EntityConfig(msg.enabled, msg.value1, msg.value2, msg.value3);
        EntityVoiceConfig.set(msg.targetId, newConfig);
        EntityVoiceConfig.persist();
    }
    
    private static void handleGeneralSoundConfig(UpdateConfigPacket msg) {
        if (GeneralSoundsConfig.ROOT == null) {
            GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
        }
        if (GeneralSoundsConfig.ROOT.sounds == null) {
            GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<>();
        }

        GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled, msg.value1, msg.value2);
        GeneralSoundsConfig.persist();

        if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Server updated GeneralSound config for: " + msg.targetId + 
                             " enabled=" + msg.enabled + " speed=" + msg.value1 + " range=" + msg.value2);
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

        if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] Server updated GeneralSound entity config for: " + msg.targetId + 
                             " enabled=" + msg.enabled + " speed=" + msg.value1 + " range=" + msg.value2);
        }
    }

    private static void handleGunfireSoundConfig(UpdateConfigPacket msg) {
        GunfireConfig.setPrioritySound(msg.targetId, msg.enabled);
        GunfireConfig.persist();
    }
    
    private static void handleGunfireEntityConfig(UpdateConfigPacket msg) {
        GunfireConfig.setMobReaction(msg.targetId, msg.enabled, msg.value1, msg.value2);
        GunfireConfig.persist();
    }
    
    private static void handleGlobalConfigChange(UpdateConfigPacket msg) {
        switch (msg.configType) {
            case GENERAL_SOUND:
                if (GeneralSoundsConfig.ROOT == null) GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
                GeneralSoundsConfig.ROOT.enabled = msg.enabled;
                GeneralSoundsConfig.persist();
                break;
            case GUNFIRE_SOUND:
                if (GunfireConfig.ROOT == null) GunfireConfig.ROOT = new GunfireConfig.Root();
                GunfireConfig.ROOT.enabled = msg.enabled;
                GunfireConfig.persist();
                break;
        }
    }

    private static void handleRefreshRequest(UpdateConfigPacket msg) {
        try {
            GeneralSoundsConfig.init();
            GunfireConfig.init();
            EntityVoiceConfig.init();
            SoundConfig.loadConfigs();
        } catch (Exception e) {
            if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
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
                            if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (NoSuchMethodError nsme) {
            if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] update packet: level.getAllEntities() does not exist in this mapping.");
                nsme.printStackTrace();
            }
        } catch (Exception e) {
            if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
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

                    ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
                    if (mobId == null) continue;

                    if (mobId.toString().equals(entityId)) {
                        if (mob instanceof IGoalRefresher refresher) {
                            try {
                                refresher.ezvcsurvival$RefreshGoals();
                            } catch (Exception ex) {
                                if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        } catch (NoSuchMethodError nsme) {
            if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] update packet: level.getAllEntities() does not exist in this mapping.");
                nsme.printStackTrace();
            }
        } catch (Exception e) {
            if (com.armilp.ezvcsurvival.config.VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Error to refresh goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
