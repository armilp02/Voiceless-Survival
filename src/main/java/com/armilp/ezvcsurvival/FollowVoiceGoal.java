package com.armilp.ezvcsurvival;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FollowVoiceGoal extends Goal {

    private final Mob mob;
    private final double speedModifier;
    private final int voiceDetectionRange;
    private Player targetPlayer;
    private final double threshold;
    private BlockPos targetSoundPosition;
    private long timePlayerInRange;
    private long lastAttackTime = 0;
    private final long attackCooldown = 2000;

    public FollowVoiceGoal(Mob mob, double speedModifier, int detectionRange, double threshold) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
        this.threshold = threshold;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        // Detecta el último sonido o un jugador cercano
        targetPlayer = getNearestPlayerInRange();
        targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
        return targetPlayer != null || targetSoundPosition != null;
    }

    @Override
    public void start() {
        if (targetPlayer != null) {
            timePlayerInRange = System.currentTimeMillis();
        } else if (targetSoundPosition != null) {
            moveToSoundPosition();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return targetPlayer != null || (targetSoundPosition != null && !mob.getNavigation().isDone());
    }

    @Override
    public void tick() {
        if (targetPlayer != null) {
            targetSoundPosition = null;
            handlePlayerInteraction();
        } else if (targetSoundPosition != null) {
            handleSoundInteraction();
        }
    }

    @Override
    public void stop() {
        targetSoundPosition = null;
        targetPlayer = null;
        mob.getNavigation().stop();
    }

    private void handlePlayerInteraction() {
        double distanceToPlayer = mob.distanceTo(targetPlayer);

        // Ignorar al jugador si está en modo creativo
        if (targetPlayer.isCreative()) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        // Si el jugador está fuera del rango de detección, reiniciar el objetivo
        if (distanceToPlayer > voiceDetectionRange) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        // Si el mob está cerca del jugador, atacar
        if (distanceToPlayer <= 1.0) {
            long currentTime = System.currentTimeMillis(); // Tiempo actual en milisegundos

            // Verificar si el mob puede atacar nuevamente
            if (currentTime - lastAttackTime >= attackCooldown) {
                mob.getNavigation().stop();
                mob.swing(mob.getUsedItemHand()); // Animación del ataque
                mob.doHurtTarget(targetPlayer);   // Realiza el daño al jugador
                lastAttackTime = currentTime;     // Actualizar el tiempo del último ataque
            }
            return;
        }

        // Si el jugador está dentro del rango, moverse hacia él
        mob.getNavigation().moveTo(targetPlayer, speedModifier);
        targetSoundPosition = null; // Ignorar sonidos mientras persigue al jugador
    }

    private void handleSoundInteraction() {
        double distanceToTarget = mob.blockPosition().distSqr(targetSoundPosition);

        if (distanceToTarget <= 1.5 * 1.5) {
            targetSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
            if (targetSoundPosition != null) {
                moveToSoundPosition();
            } else {
                mob.getNavigation().stop();
            }
            return;
        }

        if (distanceToTarget > (double) (voiceDetectionRange * voiceDetectionRange) / 2) {
            BlockPos newSoundPosition = Plugin.getLastSoundLocation(mob.blockPosition(), voiceDetectionRange);
            if (newSoundPosition == null) {
                targetSoundPosition = null;
                mob.getNavigation().stop();
                return;
            } else {
                targetSoundPosition = newSoundPosition;
                moveToSoundPosition();
            }
        }
    }

    private Player getNearestPlayerInRange() {
        return mob.level().getNearestPlayer(mob, 5);
    }

    private void moveToSoundPosition() {
        if (targetSoundPosition != null) {
            mob.getNavigation().moveTo(
                    targetSoundPosition.getX() + 0.5,
                    targetSoundPosition.getY(),
                    targetSoundPosition.getZ() + 0.5,
                    speedModifier
            );
        }
    }
}