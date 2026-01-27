package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.util.IGoalRefresher;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class ReloadConfigCommand implements ICommand {

    @Mod.EventHandler
    public static void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new ReloadConfigCommand());
    }

    @Override
    public String getName() {
        return "ezvcsurvival";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ezvcsurvival reloadconfig";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0 && args[0].equalsIgnoreCase("reloadconfig")) {
            executeReload(server, sender);
        } else {
            sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §eUsage: /ezvcsurvival reloadconfig"));
        }
    }

    private void executeReload(MinecraftServer server, ICommandSender sender) {
        try {
            sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §eReloading configuration files..."));

            // Entity Voice Config
            try {
                reloadEntityVoiceConfig();
            } catch (Exception e) {
                sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §cFailed to reload EntityVoiceConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG) {
                    e.printStackTrace();
                }
            }

            // General Sound Config
            try {
                reloadGeneralSoundsConfig();
            } catch (Exception e) {
                sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §cFailed to reload GeneralSoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG) {
                    e.printStackTrace();
                }
            }

            // Sound Config
            try {
                SoundConfig.loadConfigs();
            } catch (Exception e) {
                sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §cFailed to reload SoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG) {
                    e.printStackTrace();
                }
            }

            // Refresh Mob Goals
            try {
                refreshAllMobGoals(server, sender);
            } catch (Exception e) {
                sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §cFailed to refresh mob goals: " + e.getMessage()));
                if (VoiceConfig.DEBUG) {
                    e.printStackTrace();
                }
            }

            sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §aAll configurations reloaded successfully!"));

        } catch (Exception e) {
            sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §cAn unexpected error occurred: " + e.getMessage()));
            if (VoiceConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    private void reloadEntityVoiceConfig() throws Exception {
        Method loadOrCreateMethod = EntityVoiceConfig.class.getDeclaredMethod("loadOrCreate");
        loadOrCreateMethod.setAccessible(true);
        loadOrCreateMethod.invoke(null);
    }

    private void reloadGeneralSoundsConfig() throws Exception {
        Method loadOrCreateMethod = GeneralSoundsConfig.class.getDeclaredMethod("loadOrCreate");
        loadOrCreateMethod.setAccessible(true);
        loadOrCreateMethod.invoke(null);
    }

    private void refreshAllMobGoals(MinecraftServer server, ICommandSender sender) {
        if (server != null) {
            int totalRefreshed = 0;
            for (WorldServer level : server.worlds) {
                for (Entity entity : level.loadedEntityList) {
                    if (entity instanceof EntityLiving && entity instanceof IGoalRefresher) {
                        EntityLiving mob = (EntityLiving) entity;
                        IGoalRefresher goalRefresher = (IGoalRefresher) entity;
                        try {
                            goalRefresher.ezvcsurvival$RefreshGoals();
                            totalRefreshed++;
                        } catch (Exception e) {
                            if (VoiceConfig.DEBUG) {
                                System.err.println("[EZVCSurvival] Error refreshing goals for mob " + mob.getClass().getSimpleName() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }

            if (VoiceConfig.DEBUG) {
                System.out.println("[EZVCSurvival] Refreshed goals for " + totalRefreshed + " mobs across all levels");
            }
        } else {
            if (VoiceConfig.DEBUG) {
                System.err.println("[EZVCSurvival] Could not access server to refresh mob goals");
            }
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(2, this.getName());
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable net.minecraft.util.math.BlockPos targetPos) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<String>();
            suggestions.add("reloadconfig");
            return suggestions;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return this.getName().compareTo(o.getName());
    }
}