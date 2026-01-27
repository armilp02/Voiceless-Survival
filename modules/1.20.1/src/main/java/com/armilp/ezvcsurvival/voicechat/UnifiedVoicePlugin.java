//package com.armilp.ezvcsurvival.voicechat;
//
//import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
//import com.armilp.ezvcsurvival.config.VoiceConfig;
//import com.armilp.ezvcsurvival.data.SoundData;
//import com.armilp.ezvcsurvival.events.ArmorEventHandler;
//import com.armilp.ezvcsurvival.sculk.SculkVibrationHelper;
//import com.armilp.ezvcsurvival.util.IVoiceChatAdapter;
//import net.minecraft.core.BlockPos;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.phys.Vec3;
//import net.minecraftforge.fml.ModList;
//import net.minecraftforge.fml.common.Mod;
//
//import javax.annotation.Nullable;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.Executors;
//
//@Mod.EventBusSubscriber(modid = "ezvcsurvival")
//public class UnifiedVoicePlugin {
//
//    private static final double VOICE_ACTIVATION_THRESHOLD = -50.0;
//    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//    private static final Map<UUID, SoundData> playerSoundLocations = new ConcurrentHashMap<>();
//    private static final Map<UUID, Long> lastVoiceEffectTime = new ConcurrentHashMap<>();
//    private static final Map<UUID, Long> lastSculkVibrationTime = new ConcurrentHashMap<>();
//    private static final long DEATH_ANGELS_EFFECT_COOLDOWN_MS = 3000;
//    private static final long SCULK_VIBRATION_COOLDOWN_MS = 500;
//
//    private static IVoiceChatAdapter voiceChatAdapter;
//
//    private static boolean isDebugEnabled() {
//        try {
//            return VoiceConfig.DEBUG.get();
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    public static void initialize() {
//        if (ModList.get().isLoaded("plasmovoice")) {
//            try {
//                Class<?> adapterClass = Class.forName("com.armilp.ezvcsurvival.voicechat.PlasmoVoiceAdapter");
//                Object adapterInstance = adapterClass.getDeclaredConstructor().newInstance();
//
//                Class<?> serverClass = Class.forName("su.plo.voice.api.server.PlasmoVoiceServer");
//                Object addonsLoader = serverClass.getMethod("getAddonsLoader").invoke(null);
//                addonsLoader.getClass().getMethod("load", Object.class).invoke(addonsLoader, adapterInstance);
//
//                voiceChatAdapter = (IVoiceChatAdapter) adapterInstance;
//                System.out.println("[Voiceless Survival] Plasmo Voice initialized");
//            } catch (Exception e) {
//                System.err.println("[Voiceless Survival] Plasmo Voice found but failed to load: " + e.getMessage());
//                if (isDebugEnabled()) e.printStackTrace();
//            }
//        }
//
//        if (voiceChatAdapter == null && ModList.get().isLoaded("voicechat")) {
//            try {
//                voiceChatAdapter = new SimpleVoiceChatAdapter();
//                System.out.println("[Voiceless Survival] Simple Voice Chat initialized");
//            } catch (Exception e) {
//                System.err.println("[Voiceless Survival] Failed to initialize Simple Voice Chat: " + e.getMessage());
//                if (isDebugEnabled()) e.printStackTrace();
//            }
//        }
//
//        if (voiceChatAdapter == null) {
//            System.out.println("[Voiceless Survival] No voice chat mod detected - voice features disabled");
//            System.out.println("[Voiceless Survival] Install Simple Voice Chat or Plasmo Voice to enable voice detection");
//        }
//    }
//
//    public static boolean isVoiceChatAvailable() {
//        return voiceChatAdapter != null;
//    }
//
//    public static IVoiceChatAdapter getAdapter() {
//        return voiceChatAdapter;
//    }
//
//    public static double getMaxAudioLevel(short[] samples) {
//        if (samples == null || samples.length == 0) {
//            return -127D;
//        }
//
//        double rms = 0D;
//        for (short sample : samples) {
//            double sampleNorm = (double) sample / (double) Short.MAX_VALUE;
//            rms += sampleNorm * sampleNorm;
//        }
//
//        rms = Math.sqrt(rms / samples.length);
//
//        double db;
//        if (rms > 0D) {
//            db = Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
//        } else {
//            db = -127D;
//        }
//
//        return db;
//    }
//
//    @Nullable
//    public static BlockPos getLastSoundLocation(BlockPos mobPosition, double range, double minDb) {
//        return playerSoundLocations.values().stream()
//                .filter(data -> data.audioLevelDb() >= minDb)
//                .filter(data -> mobPosition.distSqr(data.position()) <= range * range)
//                .min(Comparator.comparingDouble(data -> mobPosition.distSqr(data.position())))
//                .map(SoundData::position)
//                .orElse(null);
//    }
//
//    public static void processVoicePacket(ServerPlayer player, short[] decodedAudio, double audioLevel,
//                                          BlockPos playerPosition, boolean isWhispering) {
//        if (audioLevel < VOICE_ACTIVATION_THRESHOLD) {
//            return;
//        }
//
//        UUID playerUUID = player.getUUID();
//        Vec3 senderVec = player.position();
//
//        double whisperRangeMultiplier = VoiceConfig.WHISPER_RANGE_MULTIPLIER.get();
//        double whisperSpeedMultiplier = VoiceConfig.WHISPER_SPEED_MULTIPLIER.get();
//        double thunderRangeMultiplier = VoiceConfig.THUNDER_RANGE_MULTIPLIER.get();
//        double sneakingRangeMultiplier = VoiceConfig.SNEAKING_RANGE_MULTIPLIER.get();
//
//        List<String> allIds = new ArrayList<>(EntityVoiceConfig.getAllEntityIds());
//        long currentTime = System.currentTimeMillis();
//
//        for (String id : allIds) {
//            EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getMonster(id);
//            if (cfg == null) cfg = EntityVoiceConfig.getAnimal(id);
//            if (cfg == null || !cfg.enabled) continue;
//
//            double threshold = cfg.threshold;
//            double detectionRange = cfg.range;
//            double speed = cfg.speed;
//
//            if (isWhispering) {
//                detectionRange *= whisperRangeMultiplier;
//                speed *= whisperSpeedMultiplier;
//            }
//
//            if (player.isCrouching()) detectionRange *= sneakingRangeMultiplier;
//            if (player.level().isRaining() || player.level().isThundering())
//                detectionRange *= thunderRangeMultiplier;
//
//            double[] armorMult = ArmorEventHandler.getArmorMultipliers(player);
//            detectionRange *= armorMult[1];
//            speed *= armorMult[0];
//
//            double distance = senderVec.distanceTo(new Vec3(playerPosition.getX(), playerPosition.getY(), playerPosition.getZ()));
//            double distanceVolume = 1.0 - Math.min(distance, detectionRange) / detectionRange;
//
//            if (audioLevel >= threshold && distanceVolume > 0.0) {
//                playerSoundLocations.put(playerUUID, new SoundData(playerPosition, audioLevel));
//
//                if (isDebugEnabled()) {
//                    System.out.println("[DEBUG] " + id + " detects sound! Threshold: " + threshold +
//                            " dB | AudioLevel: " + audioLevel + " dB | Range: " + detectionRange +
//                            " | Speed: " + speed + " | Position: " + playerPosition);
//                }
//
//                handleDeathAngelsEffects(id, audioLevel, playerUUID, currentTime, player);
//            }
//        }
//
//        handleSculkVibration(player, playerUUID, currentTime, audioLevel);
//    }
//
//    private static void handleDeathAngelsEffects(String id, double audioLevel, UUID playerUUID,
//                                                 long currentTime, ServerPlayer player) {
//        if (id.equals("death_angels:death_angel") && audioLevel >= VoiceConfig.DEATH_ANGELS_THRESHOLD.get() &&
//                (!lastVoiceEffectTime.containsKey(playerUUID) ||
//                        currentTime - lastVoiceEffectTime.get(playerUUID) > DEATH_ANGELS_EFFECT_COOLDOWN_MS)) {
//            lastVoiceEffectTime.put(playerUUID, currentTime);
//        }
//
//        if (id.equals("quiet_place:death_angel") && audioLevel >= VoiceConfig.QUIET_PLACE_OVERMAN_THRESHOLD.get() &&
//                (!lastVoiceEffectTime.containsKey(playerUUID) ||
//                        currentTime - lastVoiceEffectTime.get(playerUUID) > DEATH_ANGELS_EFFECT_COOLDOWN_MS)) {
//            lastVoiceEffectTime.put(playerUUID, currentTime);
//        }
//    }
//
//    private static void handleSculkVibration(ServerPlayer player, UUID playerUUID, long currentTime, double audioLevel) {
//        if (VoiceConfig.SCULK_SENSOR_ENABLED.get() &&
//                (!lastSculkVibrationTime.containsKey(playerUUID) ||
//                        currentTime - lastSculkVibrationTime.get(playerUUID) > SCULK_VIBRATION_COOLDOWN_MS) &&
//                audioLevel >= VoiceConfig.SCULK_SENSOR_THRESHOLD.get()) {
//            SculkVibrationHelper.generateVibration(player, VoiceConfig.SCULK_SENSOR_RANGE.get(), audioLevel);
//            lastSculkVibrationTime.put(playerUUID, currentTime);
//        }
//    }
//}