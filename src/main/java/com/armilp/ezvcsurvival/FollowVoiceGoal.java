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
    private BlockPos targetSoundPosition;
    private long timePlayerInRange;

    public FollowVoiceGoal(Mob mob, double speedModifier, int detectionRange) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.voiceDetectionRange = detectionRange;
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

        // Si el jugador sale del rango, reiniciar el seguimiento
        if (distanceToPlayer > 5.0) {
            targetPlayer = null;
            mob.getNavigation().stop();
            return;
        }

        // Si el mob está cerca del jugador (por ejemplo, rango de ataque = 1.5 bloques), atacar
        if (distanceToPlayer <= 1.5) {
            mob.getNavigation().stop();
            mob.swing(mob.getUsedItemHand()); // Animación del ataque
            mob.doHurtTarget(targetPlayer);   // Realiza el ataque
            return;
        }

        // Comprobar si el jugador lleva más de 10 segundos en rango y atacar
        if (System.currentTimeMillis() - timePlayerInRange >= 10_000) {
            mob.setTarget(targetPlayer); // Apunta al jugador como objetivo
        } else {
            mob.getNavigation().moveTo(targetPlayer, speedModifier); // Sigue al jugador
        }
    }

    private void handleSoundInteraction() {
        double distanceToTarget = mob.blockPosition().distSqr(targetSoundPosition);

        if (distanceToTarget <= 2.0 * 2.0) {
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
