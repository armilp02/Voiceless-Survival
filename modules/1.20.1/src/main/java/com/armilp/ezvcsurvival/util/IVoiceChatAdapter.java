package com.armilp.ezvcsurvival.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public interface IVoiceChatAdapter {

    String getPluginId();

    boolean isActive();

    void processVoicePacket(ServerPlayer player, short[] audioData, double audioLevel, BlockPos position, boolean isWhispering);
}