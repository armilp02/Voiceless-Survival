package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.network.CustomPayloadEvent;

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

    public static void encode(GeneralSoundPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.sound);
        buf.writeDouble(packet.x);
        buf.writeDouble(packet.y);
        buf.writeDouble(packet.z);
        buf.writeDouble(packet.speedMultiplier);
        buf.writeDouble(packet.rangeMultiplier);
    }

    public static GeneralSoundPacket decode(FriendlyByteBuf buf) {
        ResourceLocation sound = buf.readResourceLocation();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        double speedMultiplier = buf.readDouble();
        double rangeMultiplier = buf.readDouble();
        return new GeneralSoundPacket(sound, x, y, z, speedMultiplier, rangeMultiplier);
    }

    public static void handle(GeneralSoundPacket packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();
            Vec3 soundPos = new Vec3(packet.x, packet.y, packet.z);

            SoundEventTracker.setLastPlayedPosition(
                    packet.sound,
                    soundPos.x,
                    soundPos.y,
                    soundPos.z,
                    packet.speedMultiplier,
                    packet.rangeMultiplier
            );

            SoundEventTracker.notifyNearbyMobs(
                    level,
                    packet.sound,
                    soundPos.x,
                    soundPos.y,
                    soundPos.z,
                    packet.speedMultiplier,
                    packet.rangeMultiplier
            );

            var soundMap = GeneralSoundsConfig.getSounds();
            if (soundMap != null) {
                GeneralSoundsConfig.SoundEntry cfg = soundMap.get(packet.sound.toString());
                if (cfg != null && cfg.is_priority) {
                    ReactToGeneralSoundGoal.setPrioritySound(soundPos);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}