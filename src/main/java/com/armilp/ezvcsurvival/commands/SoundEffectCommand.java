package com.armilp.ezvcsurvival.commands;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraftforge.fml.ModList;

public class SoundEffectCommand {

    public static void applyEffect(ServerPlayer player) {
        if (!ModList.get().isLoaded("death_angels")) {
            return;
        }

        if (player.getServer() != null) {
            CommandSourceStack sourceStack = player.createCommandSourceStack();
            CommandSourceStack silentSource = sourceStack.withSuppressedOutput();

            String command = "effect give " + player.getGameProfile().getName() + " death_angels:sound_effect 2 1 true";
            try {
                player.getServer().getCommands().getDispatcher().execute(command, silentSource);
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}
