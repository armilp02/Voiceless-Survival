package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GeneralSoundPacket {

    private static final double MAX_DISTANCE_FROM_PLAYER = 64.0;

    private final ResourceLocation sound;
    private final double x;
    private final double y;
    private final double z;
    private final double speedMultiplier;
    private final double rangeMultiplier;

    public GeneralSoundPacket(ResourceLocation sound, double x, double y, double z,
                              double speedMultiplier, double rangeMultiplier) {
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

    public static void handle(GeneralSoundPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (!Double.isFinite(packet.x) || !Double.isFinite(packet.y) || !Double.isFinite(packet.z)) {
                return;
            }

            Vec3 soundPos = new Vec3(packet.x, packet.y, packet.z);
            Vec3 playerPos = player.position();

            if (playerPos.distanceToSqr(soundPos) > MAX_DISTANCE_FROM_PLAYER * MAX_DISTANCE_FROM_PLAYER) {
                return;
            }

            SoundEventTracker.setLastPlayedPosition(
                    packet.sound,
                    soundPos.x,
                    soundPos.y,
                    soundPos.z,
                    packet.speedMultiplier,
                    packet.rangeMultiplier
            );

            GeneralSoundsConfig.SoundEntry cfg = null;
            var soundMap = GeneralSoundsConfig.getSounds();
            if (soundMap != null) {
                cfg = soundMap.get(packet.sound.toString());
            }

            if (cfg != null && cfg.is_priority) {
                ReactToGeneralSoundGoal.setPrioritySound(soundPos);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}