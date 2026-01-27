package com.armilp.ezvcsurvival.commands;

import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.OpenConfigEditorPacket;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = "ezvcsurvival")
public class OpenConfigCommand implements ICommand {

    @Mod.EventHandler
    public static void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new OpenConfigCommand());
    }

    @Override
    public String getName() {
        return "ezvcsconfig";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/ezvcsconfig";
    }

    @Override
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            EZVCNetwork.INSTANCE.sendTo(new OpenConfigEditorPacket(), player);
            sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §aOpening config editor..."));
        } else {
            sender.sendMessage(new TextComponentString("§6[EZVCSurvival] §cThis command can only be used by players"));
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(2, this.getName());
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable net.minecraft.util.math.BlockPos targetPos) {
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