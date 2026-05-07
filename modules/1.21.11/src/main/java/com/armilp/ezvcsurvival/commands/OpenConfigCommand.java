package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.network.OpenConfigEditorPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class OpenConfigCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("ezvcsurvival")
                        .requires(source -> {
                            if (source.getEntity() instanceof ServerPlayer player) {
                                return source.getServer().getPlayerList().isOp(player.nameAndId());
                            }
                            return false;
                        })
                        .then(Commands.literal("config")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        PacketDistributor.sendToPlayer(player, new OpenConfigEditorPacket());

                                        context.getSource().sendSuccess(
                                                () -> Component.literal("Opening EZVCSurvival config editor..."),
                                                false
                                        );
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(
                                                Component.literal("This command can only be used by players")
                                        );
                                        return 0;
                                    }
                                }))
        );
    }
}