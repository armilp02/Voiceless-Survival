package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Map;

public class GeneralSoundPacket implements IMessage {
    private ResourceLocation sound;
    private double x;
    private double y;
    private double z;
    private double speedMultiplier;
    private double rangeMultiplier;

    public GeneralSoundPacket() {
    }

    public GeneralSoundPacket(ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        this.sound = sound;
        this.x = x;
        this.y = y;
        this.z = z;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.sound = new ResourceLocation(ByteBufUtils.readUTF8String(buf));
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.speedMultiplier = buf.readDouble();
        this.rangeMultiplier = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.sound.toString());
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeDouble(this.speedMultiplier);
        buf.writeDouble(this.rangeMultiplier);
    }

    public static class Handler implements IMessageHandler<GeneralSoundPacket, IMessage> {
        @Override
        public IMessage onMessage(final GeneralSoundPacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;

            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (player == null) return;

                    WorldServer level = (WorldServer) player.world;
                    Vec3d soundPos = new Vec3d(message.x, message.y, message.z);

                    SoundEventTracker.setLastPlayedPosition(message.sound, soundPos.x, soundPos.y, soundPos.z,
                            message.speedMultiplier, message.rangeMultiplier);

                    SoundEventTracker.notifyNearbyMobs(level, message.sound, soundPos.x, soundPos.y, soundPos.z,
                            message.speedMultiplier, message.rangeMultiplier);

                    Map<String, GeneralSoundsConfig.SoundEntry> soundMap = GeneralSoundsConfig.getSounds();
                    if (soundMap != null) {
                        GeneralSoundsConfig.SoundEntry cfg = soundMap.get(message.sound.toString());
                        if (cfg != null && cfg.is_priority) {
                            ReactToGeneralSoundGoal.setPrioritySound(soundPos);
                        }
                    }
                }
            });
            return null;
        }
    }
}