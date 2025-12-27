package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.util.IGoalRefresher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber
public class ReloadConfigCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ezvcsurvival")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reloadconfig")
                        .executes(ReloadConfigCommand::executeReload)
                )
        );
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            source.sendSuccess(new TextComponent("§6[EZVCSurvival] §eReloading configuration files..."), false);

            // Entity Voice Config
            try {
                reloadEntityVoiceConfig();
            } catch (Exception e) {
                source.sendFailure(new TextComponent("§6[EZVCSurvival] §cFailed to reload EntityVoiceConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // General Sound Config
            try {
                reloadGeneralSoundsConfig();
            } catch (Exception e) {
                source.sendFailure(new TextComponent("§6[EZVCSurvival] §cFailed to reload GeneralSoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // Gunfire Config
            try {
                reloadGunfireConfig();
            } catch (Exception e) {
                source.sendFailure(new TextComponent("§6[EZVCSurvival] §cFailed to reload GunfireConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // Sound Config
            try {
                SoundConfig.loadConfigs();
            } catch (Exception e) {
                source.sendFailure(new TextComponent("§6[EZVCSurvival] §cFailed to reload SoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // Refresh Mob Goals
            try {
                refreshAllMobGoals(source);
            } catch (Exception e) {
                source.sendFailure(new TextComponent("§6[EZVCSurvival] §cFailed to refresh mob goals: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            source.sendSuccess(new TextComponent("§6[EZVCSurvival] §aAll configurations reloaded successfully!"), false);
            return 1;

        } catch (Exception e) {
            source.sendFailure(new TextComponent("§6[EZVCSurvival] §cAn unexpected error occurred: " + e.getMessage()));
            if (VoiceConfig.DEBUG.get()) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    private static void reloadEntityVoiceConfig() throws Exception {
        Method loadOrCreateMethod = EntityVoiceConfig.class.getDeclaredMethod("loadOrCreate");
        loadOrCreateMethod.setAccessible(true);
        loadOrCreateMethod.invoke(null);
    }

    private static void reloadGeneralSoundsConfig() throws Exception {
        Method loadOrCreateMethod = GeneralSoundsConfig.class.getDeclaredMethod("loadOrCreate");
        loadOrCreateMethod.setAccessible(true);
        loadOrCreateMethod.invoke(null);
    }

    private static void reloadGunfireConfig() throws Exception {
        Method loadOrCreateMethod = GunfireConfig.class.getDeclaredMethod("loadOrCreate");
        loadOrCreateMethod.setAccessible(true);
        loadOrCreateMethod.invoke(null);
    }

    private static void refreshAllMobGoals(CommandSourceStack source) {
        if (source.getServer() != null) {
            int totalRefreshed = 0;
            for (ServerLevel level : source.getServer().getAllLevels()) {
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof Mob mob && entity instanceof IGoalRefresher goalRefresher) {
                        try {
                            goalRefresher.ezvcsurvival$RefreshGoals();
                            totalRefreshed++;
                        } catch (Exception e) {
                            if (VoiceConfig.DEBUG.get()) {
                                System.err.println("[EZVCSurvival] Error refreshing goals for mob " + mob.getType() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Refreshed goals for " + totalRefreshed + " mobs across all levels");
            }
        } else {
            if (VoiceConfig.DEBUG.get()) {
                System.err.println("[EZVCSurvival] Could not access server to refresh mob goals");
            }
        }
    }
}