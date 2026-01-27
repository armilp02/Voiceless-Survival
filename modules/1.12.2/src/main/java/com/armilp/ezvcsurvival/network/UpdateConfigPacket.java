package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.util.IGoalRefresher;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class UpdateConfigPacket implements IMessage {

    public enum ConfigType {
        ENTITY_VOICE(0),
        GENERAL_SOUND(1),
        GENERAL_SOUND_ENTITY(2),
        SOUND_PRIORITY(3);

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

    private ConfigType configType;
    private String targetId;
    private boolean enabled;
    private double value1;
    private double value2;
    private double value3;
    private boolean boolValue;

    public UpdateConfigPacket() {
    }

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

    @Override
    public void fromBytes(ByteBuf buf) {
        this.configType = ConfigType.fromId(buf.readInt());
        this.targetId = ByteBufUtils.readUTF8String(buf);
        this.enabled = buf.readBoolean();
        this.value1 = buf.readDouble();
        this.value2 = buf.readDouble();
        this.value3 = buf.readDouble();
        this.boolValue = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.configType.getId());
        ByteBufUtils.writeUTF8String(buf, this.targetId);
        buf.writeBoolean(this.enabled);
        buf.writeDouble(this.value1);
        buf.writeDouble(this.value2);
        buf.writeDouble(this.value3);
        buf.writeBoolean(this.boolValue);
    }

    public static class Handler implements IMessageHandler<UpdateConfigPacket, IMessage> {
        @Override
        public IMessage onMessage(final UpdateConfigPacket message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    boolean configChanged = false;

                    if ("global".equals(message.targetId)) {
                        handleGlobalConfigChange(message);
                        configChanged = true;
                    } else if ("refresh".equals(message.targetId)) {
                        handleRefreshRequest(message);
                        configChanged = true;
                    } else {
                        switch (message.configType) {
                            case ENTITY_VOICE:
                                handleEntityVoiceConfig(message);
                                configChanged = true;
                                break;
                            case GENERAL_SOUND:
                                handleGeneralSoundConfig(message);
                                configChanged = true;
                                break;
                            case GENERAL_SOUND_ENTITY:
                                handleGeneralSoundEntityConfig(message);
                                configChanged = true;
                                break;
                            case SOUND_PRIORITY:
                                handleSoundPriorityConfig(message);
                                configChanged = true;
                                break;
                        }
                    }

                    if (configChanged) {
                        try {
                            switch (message.configType) {
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

                            if (VoiceConfig.DEBUG) {
                                System.out.println("[EZVCSurvival] Configurations reloaded successfully after update: " + message.configType + " - " + message.targetId);
                            }
                        } catch (Exception e) {
                            if (VoiceConfig.DEBUG) {
                                System.err.println("[EZVCSurvival] Error reloading configurations after update: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        refreshAllEntityGoals();

                        if (message.configType == ConfigType.ENTITY_VOICE ||
                                message.configType == ConfigType.GENERAL_SOUND_ENTITY ||
                                message.configType == ConfigType.SOUND_PRIORITY) {
                            refreshSpecificEntityGoals(message.targetId);
                        }
                    }
                }
            });
            return null;
        }
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

        if (VoiceConfig.DEBUG) {
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
            GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<String, GeneralSoundsConfig.SoundEntry>();
        }

        GeneralSoundsConfig.setSoundEntry(msg.targetId, msg.enabled, msg.value1, msg.value2, msg.boolValue);
        GeneralSoundsConfig.persist();

        GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(msg.targetId);
        boolean verificationPassed = (updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed_multiplier - msg.value1) < 0.001 &&
                Math.abs(updated.range_multiplier - msg.value2) < 0.001 &&
                updated.is_priority == msg.boolValue);

        if (VoiceConfig.DEBUG) {
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
            GeneralSoundsConfig.ROOT.mobs = new java.util.HashMap<String, GeneralSoundsConfig.Reaction>();
        }

        GeneralSoundsConfig.setMobReaction(msg.targetId, msg.enabled, msg.value1, msg.value2);
        GeneralSoundsConfig.persist();

        GeneralSoundsConfig.Reaction updated = GeneralSoundsConfig.getMobReactions().get(msg.targetId);
        boolean verificationPassed = (updated != null &&
                updated.enabled == msg.enabled &&
                Math.abs(updated.speed - msg.value1) < 0.001 &&
                Math.abs(updated.range - msg.value2) < 0.001);

        if (VoiceConfig.DEBUG) {
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
            GeneralSoundsConfig.ROOT.sounds = new java.util.HashMap<String, GeneralSoundsConfig.SoundEntry>();
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

        if (VoiceConfig.DEBUG) {
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

                if (VoiceConfig.DEBUG) {
                    System.out.println("[EZVCSurvival] Server updated EntityVoice global config: enabled=" + msg.enabled);
                }
                break;
            case GENERAL_SOUND:
                if (GeneralSoundsConfig.ROOT == null) {
                    GeneralSoundsConfig.ROOT = new GeneralSoundsConfig.Root();
                }
                GeneralSoundsConfig.ROOT.enabled = msg.enabled;
                GeneralSoundsConfig.persist();

                if (VoiceConfig.DEBUG) {
                    System.out.println("[EZVCSurvival] Server updated GeneralSound global config: enabled=" + msg.enabled);
                }
                break;
            case SOUND_PRIORITY:
                GeneralSoundsConfig.enableAllPrioritySounds(msg.enabled);

                if (VoiceConfig.DEBUG) {
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
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Error refreshing configs: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void refreshAllEntityGoals() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        if (server == null) {
            return;
        }

        try {
            for (WorldServer world : server.worlds) {
                for (Entity e : world.loadedEntityList) {
                    if (!(e instanceof EntityLiving)) continue;
                    EntityLiving mob = (EntityLiving) e;

                    if (mob instanceof IGoalRefresher) {
                        IGoalRefresher refresher = (IGoalRefresher) mob;
                        try {
                            refresher.ezvcsurvival$RefreshGoals();
                        } catch (Exception ex) {
                            if (VoiceConfig.DEBUG) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Error to refresh goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void refreshSpecificEntityGoals(String entityId) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        if (server == null) {
            return;
        }

        try {
            for (WorldServer world : server.worlds) {
                for (Entity e : world.loadedEntityList) {
                    if (!(e instanceof EntityLiving)) continue;
                    EntityLiving mob = (EntityLiving) e;

                    EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(entityId));
                    if (entry == null) continue;

                    Class<? extends Entity> entityClass = entry.getEntityClass();
                    if (entityClass != null && entityClass.isInstance(mob)) {
                        if (mob instanceof IGoalRefresher) {
                            IGoalRefresher refresher = (IGoalRefresher) mob;
                            try {
                                refresher.ezvcsurvival$RefreshGoals();
                            } catch (Exception ex) {
                                if (VoiceConfig.DEBUG) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Error to refresh goals: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}