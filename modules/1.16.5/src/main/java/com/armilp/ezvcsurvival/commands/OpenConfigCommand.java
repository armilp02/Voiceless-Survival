package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.OpenConfigEditorPacket;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber
public class OpenConfigCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("ezvcsurvival")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("config")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayerEntity) {
                                        ServerPlayerEntity player = (ServerPlayerEntity) context.getSource().getEntity();
                                        EZVCNetwork.INSTANCE.send(
                                                PacketDistributor.PLAYER.with(() -> player),
                                                new OpenConfigEditorPacket()
                                        );
                                        context.getSource().sendSuccess(
                                                new StringTextComponent("§6[EZVCSurvival] §aOpening config editor..."),
                                                false
                                        );
                                        return 1;
                                    } else {
                                        context.getSource().sendFailure(
                                                new StringTextComponent("§6[EZVCSurvival] §cThis command can only be used by players")
                                        );
                                        return 0;
                                    }
                                })
                        )
        );
    }
}