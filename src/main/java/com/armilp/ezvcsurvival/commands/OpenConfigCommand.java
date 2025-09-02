package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.OpenConfigEditorPacket;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber
public class OpenConfigCommand {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ezvcconfig")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        EZVCNetwork.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new OpenConfigEditorPacket()
                        );
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
                })
        );
    }
}