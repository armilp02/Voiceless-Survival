package com.armilp.ezvcsurvival.voicechat;

import com.armilp.ezvcsurvival.commands.AggroVoiceEffectCommand;
import com.armilp.ezvcsurvival.commands.SoundEffectCommand;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.data.SoundData;
import com.armilp.ezvcsurvival.events.ArmorEventHandler;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.VoiceLevelPacket;
import com.armilp.ezvcsurvival.sculk.SculkVibrationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;

public final class VoiceProcessor {

    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            });
    private static final Map<UUID, SoundData> playerSoundLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastVoiceEffectTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastSculkVibrationTime = new ConcurrentHashMap<>();
    private static final long DEATH_ANGELS_EFFECT_COOLDOWN_MS = 3000;
    private static final long SCULK_VIBRATION_COOLDOWN_MS = 500;

    private VoiceProcessor() {
    }

    public static double getMaxAudioLevel(short[] samples) {
        double rms = 0D;

        for (int i = 0; i < samples.length; i++) {
            double sample = (double) samples[i] / (double) Short.MAX_VALUE;
            rms += sample * sample;
        }

        int sampleCount = samples.length / 2;
        rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

        double db;
        if (rms > 0D) {
            db = Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
        } else {
            db = -127D;
        }

        return db;
    }

    @Nullable
    public static BlockPos getLastSoundLocation(BlockPos mobPosition, double range, double minDb) {
        return playerSoundLocations.values().stream()
                .filter(data -> data.audioLevelDb() >= minDb)
                .filter(data -> mobPosition.distSqr(data.position()) <= range * range)
                .min(Comparator.comparingDouble(data -> mobPosition.distSqr(data.position())))
                .map(SoundData::position)
                .orElse(null);
    }

    public static void processAudio(ServerPlayer player, Vec3 senderVec, double audioLevel, boolean isWhispering) {
        final boolean debug = VoiceConfig.DEBUG.get();

        EZVCNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new VoiceLevelPacket(audioLevel));

        UUID playerUUID = player.getUUID();

        BlockPos playerPosition = new BlockPos(
                (int) Math.floor(senderVec.x),
                (int) Math.floor(senderVec.y),
                (int) Math.floor(senderVec.z)
        );

        double whisperRangeMultiplier = VoiceConfig.WHISPER_RANGE_MULTIPLIER.get();
        double whisperSpeedMultiplier = VoiceConfig.WHISPER_SPEED_MULTIPLIER.get();
        double thunderRangeMultiplier = VoiceConfig.THUNDER_RANGE_MULTIPLIER.get();
        double sneakingRangeMultiplier = VoiceConfig.SNEAKING_RANGE_MULTIPLIER.get();

        List<String> allIds = new ArrayList<>(EntityVoiceConfig.getAllEntityIds());

        long currentTime = System.currentTimeMillis();

        for (String id : allIds) {
            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(id);
            if (cfg == null) cfg = EntityVoiceConfig.getAnimal(id);
            if (cfg == null || !cfg.enabled) continue;
            double threshold = cfg.threshold;
            double detectionRange = cfg.range;
            double speed = cfg.speed;

            if (isWhispering) {
                detectionRange *= whisperRangeMultiplier;
                speed *= whisperSpeedMultiplier;
            }

            if (player.isCrouching()) detectionRange *= sneakingRangeMultiplier;
            if (player.level().isRaining() || player.level().isThundering())
                detectionRange *= thunderRangeMultiplier;
            double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
            detectionRange *= armorMult[1];
            speed *= armorMult[0];

            double modifiedRange = detectionRange;

            double distance = senderVec.distanceTo(new Vec3(playerPosition.getX(), playerPosition.getY(), playerPosition.getZ()));
            double distanceVolume = 1.0 - Math.min(distance, modifiedRange) / modifiedRange;

            if (audioLevel >= threshold && distanceVolume > 0.0) {
                BlockPos precisePos = new BlockPos(
                        (int) Math.floor(senderVec.x),
                        (int) Math.floor(senderVec.y),
                        (int) Math.floor(senderVec.z)
                );
                playerSoundLocations.put(
                        playerUUID,
                        new SoundData(precisePos, audioLevel)
                );
                if (debug) {
                    System.out.println("[DEBUG] " + id + " detects sound! " +
                            "Threshold: " + threshold + " dB | " +
                            "AudioLevel: " + audioLevel + " dB | " +
                            "Range: " + detectionRange + " | " +
                            "Speed: " + speed + " | " +
                            "Position: " + precisePos);
                }

                if (id.equals("death_angels:death_angel") &&
                        (audioLevel >= VoiceConfig.DEATH_ANGELS_THRESHOLD.get()) &&
                        (!lastVoiceEffectTime.containsKey(playerUUID)
                                || currentTime - lastVoiceEffectTime.get(playerUUID) > DEATH_ANGELS_EFFECT_COOLDOWN_MS)) {
                    SoundEffectCommand.applyEffect(player);
                    lastVoiceEffectTime.put(playerUUID, currentTime);
                    if (debug) {
                        System.out.println("[DEBUG] Effect applied to the player " + playerUUID);
                    }
                }

                if (id.equals("quiet_place:death_angel") &&
                        (audioLevel >= VoiceConfig.QUIET_PLACE_OVERMAN_THRESHOLD.get()) &&
                        (!lastVoiceEffectTime.containsKey(playerUUID)
                                || currentTime - lastVoiceEffectTime.get(playerUUID) > DEATH_ANGELS_EFFECT_COOLDOWN_MS)) {
                    AggroVoiceEffectCommand.applyEffect(player);
                    lastVoiceEffectTime.put(playerUUID, currentTime);
                    if (debug) {
                        System.out.println("[DEBUG] Effect applied to the player " + playerUUID);
                    }
                }
            } else {
                if (debug) {
                    System.out.println("[DEBUG] Intensity/range too low for " + id + ": "
                            + audioLevel + " dB | " + distanceVolume);
                }
            }
        }

        if (VoiceConfig.SCULK_SENSOR_ENABLED.get()) {
            if (!lastSculkVibrationTime.containsKey(playerUUID)
                    || currentTime - lastSculkVibrationTime.get(playerUUID) > SCULK_VIBRATION_COOLDOWN_MS) {
                if (audioLevel >= VoiceConfig.SCULK_SENSOR_THRESHOLD.get()) {
                    SculkVibrationHelper.generateVibration(player, VoiceConfig.SCULK_SENSOR_RANGE.get(), audioLevel);
                    lastSculkVibrationTime.put(playerUUID, currentTime);
                    if (debug) {
                        System.out.println("[DEBUG] Sculk vibration generated for player " + playerUUID +
                                " | AudioLevel: " + audioLevel + " dB");
                    }
                }
            }
        }

        try {
            scheduler.schedule(() -> playerSoundLocations.remove(playerUUID), 5, TimeUnit.SECONDS);
        } catch (RejectedExecutionException ignored) {
        }
    }
}