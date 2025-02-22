package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.events.GunFireListener;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReactToSoundGoal extends Goal {
    private final MobEntity mob;
    private final double speed;
    private final int range;
    private final List<SoundGroupData> soundGroups;

    private Vector3d lastAttackerPos = null;
    private static final List<ReactToSoundGoal> activeGoals = new CopyOnWriteArrayList<>();

    public ReactToSoundGoal(MobEntity mob, double speed, int range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        activeGoals.add(this);
    }

    @Override
    public boolean canUse() {
        Vector3d mobCenterPos = new Vector3d(mob.getX(), mob.getY(), mob.getZ());
        Vector3d soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;

        // Primero se intenta obtener la posición del último disparo
        soundEventPos = GunFireListener.getLastGunshotPosition();

        // Si no hay disparo registrado, se recorre la lista de sonidos configurados
        if (soundEventPos == null) {
            outer:
            for (SoundGroupData group : soundGroups) {
                for (String soundStr : group.sounds) {
                    ResourceLocation res = new ResourceLocation(soundStr);
                    Vector3d pos = SoundEventTracker.getLastPlayedPositionForSound(res);
                    if (pos != null) {
                        soundEventPos = pos;
                        groupSpeedMult = group.speedMultiplier;
                        groupRangeMult = group.rangeMultiplier;
                        break outer;
                    }
                }
            }
        }

        boolean soundTriggered = false;
        if (soundEventPos != null) {
            double groupEffectiveRange = range * groupRangeMult;
            soundTriggered = mobCenterPos.distanceTo(soundEventPos) <= groupEffectiveRange;
        }

        // También se activa si el mob fue dañado recientemente (y no es un monstruo)
        boolean hurtTriggered = !(mob instanceof MonsterEntity) && lastAttackerPos != null;

        return soundTriggered || hurtTriggered;
    }

    @Override
    public void start() {
        updateNavigation();
    }

    @Override
    public void tick() {
        updateNavigation();
    }

    @Override
    public void stop() {
        activeGoals.remove(this);
    }

    public void onHurt(Vector3d attackerPos) {
        this.lastAttackerPos = attackerPos;
    }

    private void updateNavigation() {
        Vector3d currentPos = new Vector3d(mob.getX(), mob.getY(), mob.getZ());
        Vector3d target = null;
        double effectiveRange = range;
        double effectiveSpeed = speed;

        Vector3d soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;

        // Primero se intenta obtener la posición del último disparo
        soundEventPos = GunFireListener.getLastGunshotPosition();

        // Si no hay disparo, se usan los sonidos configurados
        if (soundEventPos == null) {
            outer:
            for (SoundGroupData group : soundGroups) {
                for (String soundStr : group.sounds) {
                    ResourceLocation res = new ResourceLocation(soundStr);
                    Vector3d pos = SoundEventTracker.getLastPlayedPositionForSound(res);
                    if (pos != null) {
                        soundEventPos = pos;
                        groupSpeedMult = group.speedMultiplier;
                        groupRangeMult = group.rangeMultiplier;
                        break outer;
                    }
                }
            }
        } else {
            // Se puede ajustar la velocidad y rango para el disparo, o dejarlos por defecto
            // En este ejemplo se mantienen los valores por defecto (1.0)
        }

        if (soundEventPos != null) {
            effectiveRange = (int) (range * groupRangeMult);
            effectiveSpeed = speed * groupSpeedMult;
        }

        if (mob instanceof MonsterEntity) {
            MonsterEntity monster = (MonsterEntity) mob;
            // Si el objetivo actual es un jugador, no se modifica el comportamiento
            LivingEntity targetEntity = monster.getTarget();
            if (targetEntity instanceof PlayerEntity) {
                return;
            }
            if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
                target = new Vector3d(soundEventPos.x, currentPos.y, soundEventPos.z);
            }
        } else {
            if (lastAttackerPos != null) {
                Vector3d diff = currentPos.subtract(lastAttackerPos);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vector3d(1, 0, 0);
                }
                target = currentPos.add(diff.normalize().scale(effectiveRange));
                lastAttackerPos = null;
            } else if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
                Vector3d diff = currentPos.subtract(soundEventPos);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vector3d(1, 0, 0);
                }
                target = currentPos.add(diff.normalize());
            }
        }

        if (target != null) {
            // Se asegura que la coordenada Y del objetivo sea la del mob
            target = new Vector3d(target.x, currentPos.y, target.z);
            mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
        }
    }

    @Mod.EventBusSubscriber(modid = "ezvcsurvival")
    public static class ReactToSoundGoalEventHandler {
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            for (ReactToSoundGoal goal : activeGoals) {
                if (event.getEntity() == goal.mob && !(goal.mob instanceof MonsterEntity)) {
                    if (event.getSource().getEntity() instanceof LivingEntity) {
                        LivingEntity source = (LivingEntity) event.getSource().getEntity();
                        goal.onHurt(new Vector3d(source.getX(), source.getY(), source.getZ()));
                    }
                }
            }
        }
    }
}
