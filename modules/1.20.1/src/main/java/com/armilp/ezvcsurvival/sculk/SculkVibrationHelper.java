package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SculkVibrationHelper {

    public static void generateVibration(ServerPlayer player, int distance, double audioLevel) {
        if (player == null || player.level() == null) {
            return;
        }

        player.getServer().execute(() -> {
            try {
                generateVibrationSync(player, distance, audioLevel);
            } catch (Exception e) {
                EZVCSurvival.LOGGER.error("Error generating sculk vibration", e);
            }
        });
    }

    private static void generateVibrationSync(ServerPlayer player, int distance, double audioLevel) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (ModGameEvent.VOICE_TALK == null) {
            return;
        }

        BlockPos playerPos = player.blockPosition();
        Vec3 sourcePos = player.position().add(0, 0.5, 0);
        GameEvent event = ModGameEvent.VOICE_TALK;

        List<SculkSensorBlockEntity> sensors = findNearbySculkSensors(level, playerPos, distance);

        for (SculkSensorBlockEntity sensor : sensors) {
            try {
                VibrationSystem.Listener listener = sensor.getListener();
                if (listener != null) {
                    listener.forceScheduleVibration(
                            level,
                            event,
                            new GameEvent.Context(player, null),
                            sourcePos
                    );
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.error("Failed to activate sensor", e);
            }
        }
    }

    private static List<SculkSensorBlockEntity> findNearbySculkSensors(ServerLevel level, BlockPos center, int distance) {
        List<SculkSensorBlockEntity> sensors = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-distance, -distance, -distance),
                center.offset(distance, distance, distance)
        )) {
            BlockEntity be = level.getBlockEntity(pos.immutable());
            if (be instanceof SculkSensorBlockEntity sensor) {
                sensors.add(sensor);
            }
        }

        return sensors;
    }
}