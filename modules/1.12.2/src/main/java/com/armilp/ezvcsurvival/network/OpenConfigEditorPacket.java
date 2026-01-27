package com.armilp.ezvcsurvival.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class OpenConfigEditorPacket implements IMessage {

    public OpenConfigEditorPacket() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<OpenConfigEditorPacket, IMessage> {
        @Override
        public IMessage onMessage(OpenConfigEditorPacket message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable() {
                @Override
                @SideOnly(Side.CLIENT)
                public void run() {
                    ClientPacketHandlers.handleOpenConfigEditor();
                }
            });
            return null;
        }
    }
}