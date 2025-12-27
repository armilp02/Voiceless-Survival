package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class GeneralSoundPacket {
    private final ResourceLocation sound;
    private final double x;
    private final double y;
    private final double z;
    private final double speedMultiplier;
    private final double rangeMultiplier;

    public GeneralSoundPacket(ResourceLocation sound, double x, double y, double z, double speedMultiplier, double rangeMultiplier) {
        this.sound = sound;
        this.x = x;
        this.y = y;
        this.z = z;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
    }

    public static void encode(GeneralSoundPacket packet, PacketBuffer buf) {
        buf.writeResourceLocation(packet.sound);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeDouble(packet.speedMultiplier);
        buf.writeDouble(packet.rangeMultiplier);
    }

    public static GeneralSoundPacket decode(PacketBuffer buf) {
        ResourceLocation sound = buf.readResourceLocation();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        double speedMultiplier = buf.readDouble();
        double rangeMultiplier = buf.readDouble();
        return new GeneralSoundPacket(sound, x, y, z, speedMultiplier, rangeMultiplier);
    }

    public static void handle(GeneralSoundPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(new Runnable() {
            @Override
            public void run() {
                ServerPlayerEntity player = ctx.get().getSender();
                if (player == null) return;

                ServerWorld level = (ServerWorld) player.level;
                Vector3d soundPos = new Vector3d(packet.x, packet.y, packet.z);

                SoundEventTracker.setLastPlayedPosition(packet.sound, soundPos.x, soundPos.y, soundPos.z, packet.speedMultiplier, packet.rangeMultiplier);

                SoundEventTracker.notifyNearbyMobs(level, packet.sound, soundPos.x, soundPos.y, soundPos.z, packet.speedMultiplier, packet.rangeMultiplier);

                Map<String, GeneralSoundsConfig.SoundEntry> soundMap = GeneralSoundsConfig.getSounds();
                if (soundMap != null) {
                    GeneralSoundsConfig.SoundEntry cfg = soundMap.get(packet.sound.toString());
                    if (cfg != null && cfg.is_priority) {
                        ReactToGeneralSoundGoal.setPrioritySound(soundPos);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}