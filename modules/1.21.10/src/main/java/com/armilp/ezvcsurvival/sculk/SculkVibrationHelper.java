package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SculkVibrationHelper {

    public static void generateVibration(ServerPlayer player, int distance, double audioLevel) {
        if (player == null) {
            return;
        }

        Objects.requireNonNull(player.level().getServer()).execute(() -> {
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

        BlockPos playerPos = player.blockPosition();
        Vec3 sourcePos = player.position().add(0, 0.5, 0);

        Holder<GameEvent> eventHolder = selectEventByFrequency();

        List<SculkSensorBlockEntity> sensors = findNearbySculkSensors(level, playerPos, distance);

        for (SculkSensorBlockEntity sensor : sensors) {
            try {
                VibrationSystem.Listener listener = sensor.getListener();
                if (listener != null) {
                    listener.forceScheduleVibration(
                            level,
                            eventHolder,
                            new GameEvent.Context(player, null),
                            sourcePos
                    );
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.error("Failed to activate sensor", e);
            }
        }
    }

    private static Holder<GameEvent> selectEventByFrequency() {
        int desiredFrequency = VoiceConfig.SCULK_SENSOR_FREQUENCY.get();

        return switch (desiredFrequency) {
            case 1 -> GameEvent.STEP;
            case 2 -> GameEvent.SWIM;
            case 3 -> GameEvent.ELYTRA_GLIDE;
            case 4 -> GameEvent.HIT_GROUND;
            case 5 -> GameEvent.SPLASH;
            case 6 -> GameEvent.PROJECTILE_SHOOT;
            case 7 -> GameEvent.ITEM_INTERACT_FINISH;
            case 8 -> GameEvent.BLOCK_PLACE;
            case 9 -> GameEvent.ENTITY_PLACE;
            case 10 -> GameEvent.BLOCK_ACTIVATE;
            case 11 -> GameEvent.ENTITY_INTERACT;
            case 12 -> GameEvent.INSTRUMENT_PLAY;
            case 13 -> GameEvent.ENTITY_DAMAGE;
            case 14 -> GameEvent.DRINK;
            case 15 -> GameEvent.SHRIEK;
            default -> GameEvent.STEP;
        };
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