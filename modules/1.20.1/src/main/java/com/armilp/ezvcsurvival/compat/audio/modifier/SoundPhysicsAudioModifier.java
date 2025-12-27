//package com.armilp.ezvcsurvival.compat.audio.modifier;
//
//import com.armilp.ezvcsurvival.util.IAudioModifier;
//import com.armilp.ezvcsurvival.config.VoiceConfig;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.phys.Vec3;
//
//public class SoundPhysicsAudioModifier implements IAudioModifier {
//
//    private static final double MIN_INTENSITY = 0.1;
//    private static final double MAX_INTENSITY = 2.5;
//    private static final double OCCLUSION_PENALTY = 0.15;
//    private static final double AIR_ABSORPTION = 0.02;
//
//    private final double occlusion;
//    private final double distance;
//    private final Level level;
//    private final Vec3 playerPos;
//    private final Vec3 senderPos;
//    private final boolean isValid;
//
//    private Double cachedIntensity = null;
//    private Double cachedRange = null;
//
//    public SoundPhysicsAudioModifier(double occlusion, Level level, Vec3 playerPos, Vec3 senderPos) {
//        this.occlusion = Math.max(0.0, occlusion);
//        this.level = level;
//        this.playerPos = playerPos;
//        this.senderPos = senderPos;
//        this.distance = playerPos.distanceTo(senderPos);
//
//        this.isValid = level != null && !Double.isNaN(distance) && !Double.isInfinite(distance);
//    }
//
//    @Override
//    public boolean isValid() {
//        return isValid;
//    }
//
//    @Override
//    public double getIntensityFactor() {
//        if (!isValid) {
//            return 1.0;
//        }
//
//        if (cachedIntensity != null) {
//            return cachedIntensity;
//        }
//
//        try {
//            double baseIntensity = 1.0 / (1.0 + occlusion * OCCLUSION_PENALTY);
//            double airAbsorption = Math.exp(-distance * AIR_ABSORPTION);
//            double environmentalFactor = calculateEnvironmentalFactor();
//            double intensity = baseIntensity * airAbsorption * environmentalFactor;
//
//            cachedIntensity = Math.max(MIN_INTENSITY, Math.min(MAX_INTENSITY, intensity));
//            return cachedIntensity;
//
//        } catch (Exception e) {
//            cachedIntensity = 1.0;
//            return 1.0;
//        }
//    }
//
//    @Override
//    public double computeModifiedRange(double baseRange) {
//        if (!isValid) {
//            return baseRange;
//        }
//
//        if (cachedRange != null) {
//            return cachedRange;
//        }
//
//        try {
//            double intensityFactor = getIntensityFactor();
//            double configMultiplier = VoiceConfig.SOUND_PHYSICS_INTENSITY_MULTIPLIER.get();
//            double rangeFactor = 1.0 + Math.log1p((intensityFactor * configMultiplier) - 1.0) * 0.5;
//            double occlusionRangeFactor = 1.0 / (1.0 + occlusion * 0.1);
//            double modifiedRange = baseRange * rangeFactor * occlusionRangeFactor;
//
//            double minRange = baseRange * 0.3;
//            double maxRange = baseRange * 2.0;
//
//            cachedRange = Math.max(minRange, Math.min(maxRange, modifiedRange));
//            return cachedRange;
//
//        } catch (Exception e) {
//            cachedRange = baseRange;
//            return baseRange;
//        }
//    }
//
//    private double calculateEnvironmentalFactor() {
//        try {
//            double factor = 1.0;
//
//            if (hasLineOfSight()) {
//                factor *= 1.2;
//            } else {
//                factor *= 0.8;
//            }
//
//            if (level.isRaining()) {
//                factor *= 0.85;
//            }
//
//            if (level.isThundering()) {
//                factor *= 0.75;
//            }
//
//            if (isUnderground(playerPos)) {
//                factor *= 0.9;
//            }
//
//            return Math.max(0.5, Math.min(1.5, factor));
//
//        } catch (Exception e) {
//            return 1.0;
//        }
//    }
//
//    private boolean hasLineOfSight() {
//        try {
//            Vec3 direction = playerPos.subtract(senderPos).normalize();
//            double checkDistance = Math.min(distance, 16.0);
//            int checks = (int) Math.max(1, Math.min(checkDistance, 8));
//
//            for (int i = 1; i <= checks; i++) {
//                double t = (i / (double) checks) * checkDistance;
//                Vec3 checkPos = senderPos.add(direction.scale(t));
//                BlockPos checkBlockPos = new BlockPos((int) Math.floor(checkPos.x),
//                                                      (int) Math.floor(checkPos.y),
//                                                      (int) Math.floor(checkPos.z));
//
//                BlockState state = level.getBlockState(checkBlockPos);
//                if (!state.isAir() && state.isSolidRender(level, checkBlockPos)) {
//                    return false;
//                }
//            }
//
//            return true;
//
//        } catch (Exception e) {
//            return true;
//        }
//    }
//
//    private boolean isUnderground(Vec3 pos) {
//        try {
//            BlockPos blockPos = new BlockPos((int) Math.floor(pos.x),
//                                            (int) Math.floor(pos.y),
//                                            (int) Math.floor(pos.z));
//            return !level.canSeeSky(blockPos);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//}