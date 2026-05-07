package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.events.MobGoalInjector;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.lang.reflect.Method;

@EventBusSubscriber
public class ReloadConfigCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ezvcsurvival")
                .requires(source -> {
                    if (source.getEntity() instanceof ServerPlayer player) {
                        return source.getServer().getPlayerList().isOp(player.nameAndId());
                    }
                    return false;
                })
                .then(Commands.literal("reloadconfig")
                        .executes(ReloadConfigCommand::executeReload)
                )
        );
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            source.sendSuccess(() -> Component.literal("[EZVCSurvival] Reloading configuration files..."), false);

            // Entity Voice Config
            try {
                reloadEntityVoiceConfig();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] Failed to reload EntityVoiceConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // General Sound Config
            try {
                reloadGeneralSoundsConfig();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] Failed to reload GeneralSoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // Sound Config
            try {
                SoundConfig.loadConfigs();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] Failed to reload SoundConfig: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            // Refresh Mob Goals
            try {
                MobGoalInjector.refreshAll();
            } catch (Exception e) {
                source.sendFailure(Component.literal("[EZVCSurvival] Failed to refresh mob goals: " + e.getMessage()));
                if (VoiceConfig.DEBUG.get()) {
                    e.printStackTrace();
                }
            }

            source.sendSuccess(() -> Component.literal("[EZVCSurvival] All configurations reloaded successfully!"), false);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("[EZVCSurvival] §cAn unexpected error occurred: " + e.getMessage()));
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
}