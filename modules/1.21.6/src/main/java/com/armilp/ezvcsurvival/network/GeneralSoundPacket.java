package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.goals.ReactToGeneralSoundGoal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GeneralSoundPacket(
        ResourceLocation sound,
        double x,
        double y,
        double z,
        double speedMultiplier,
        double rangeMultiplier
) implements CustomPacketPayload {

    public static final Type<GeneralSoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EZVCSurvival.MOD_ID, "general_sound"));

    public static final StreamCodec<ByteBuf, GeneralSoundPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            GeneralSoundPacket::sound,
            ByteBufCodecs.DOUBLE,
            GeneralSoundPacket::x,
            ByteBufCodecs.DOUBLE,
            GeneralSoundPacket::y,
            ByteBufCodecs.DOUBLE,
            GeneralSoundPacket::z,
            ByteBufCodecs.DOUBLE,
            GeneralSoundPacket::speedMultiplier,
            ByteBufCodecs.DOUBLE,
            GeneralSoundPacket::rangeMultiplier,
            GeneralSoundPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GeneralSoundPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow().isServerbound()) {
                // Manejar en el servidor
                ServerPlayer player = (ServerPlayer) ctx.player();
                if (player == null) return;

                ServerLevel level = player.level().getServer().overworld();
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
            }
        });
    }
}