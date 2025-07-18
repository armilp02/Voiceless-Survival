package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.data.GunshotData;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.events.GunFireListener;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.config.SoundConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReactToSoundGoal extends Goal {
    private final Mob mob;
    private final double speed;
    private final int range;
    private final List<SoundGroupData> soundGroups;

    private Vec3 lastAttackerPos = null;
    private static final List<ReactToSoundGoal> activeGoals = new CopyOnWriteArrayList<>();

    public static Vec3 lastPointBlankSoundPos = null;
    public static long lastPointBlankSoundTimestamp = 0;
    public static String lastPointBlankGunType = null;
    private static final long POINT_BLANK_SOUND_EXPIRATION_MS = 5000;

    private Vec3 lastPrioritySoundPos = null;
    private long lastPrioritySoundTimestamp = 0L;
    private static final long PRIORITY_SOUND_PERSISTENCE_MS = 7000;


    public ReactToSoundGoal(Mob mob, double speed, int range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        activeGoals.add(this);
    }

    @Override
    public boolean canUse() {
        if (lastPointBlankSoundPos != null && System.currentTimeMillis() - lastPointBlankSoundTimestamp > POINT_BLANK_SOUND_EXPIRATION_MS) {
            lastPointBlankSoundPos = null;
            lastPointBlankGunType = null;
        }

        Vec3 mobCenterPos = mob.position();
        double effectiveRange = range;
        double effectiveSpeed = speed;
        String effectiveGunType = null;

        GunshotData gunshotData = GunFireListener.getLastGunshotData();
        if (gunshotData != null) {
            effectiveGunType = gunshotData.gunType.name().toLowerCase();
        } else if (lastPointBlankSoundPos != null && lastPointBlankGunType != null) {
            effectiveGunType = lastPointBlankGunType;
        }

        if (effectiveGunType != null) {
            double rangeMultiplier;
            double speedMultiplier;
            if (lastPointBlankSoundPos != null) {
                rangeMultiplier = SoundConfig.getPointBlankRangeMultiplier(effectiveGunType);
                speedMultiplier = SoundConfig.getPointBlankSpeedMultiplier(effectiveGunType);
            } else {
                rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
            }
            effectiveRange = (int)(range * rangeMultiplier);
            effectiveSpeed = speed * speedMultiplier;
        }

        if (mob.level().isRaining() || mob.level().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        // Priority groups override
        for (SoundGroupData priority : SoundConfig.getPriorityGroups()) {
            for (String soundStr : priority.sounds) {
                ResourceLocation loc = toLocation(soundStr);
                Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(loc);
                if (pos != null) {
                    double prRange = priority.rangeMultiplier * range;
                    if (mob.level().isRaining() || mob.level().isThundering()) {
                        prRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
                    }
                    return mobCenterPos.distanceTo(pos) <= prRange;
                }
            }
        }

        boolean gunshotTriggered = gunshotData != null && mobCenterPos.distanceTo(gunshotData.position) <= effectiveRange;

        Vec3 soundEventPos = null;
        double groupRangeMult = 1.0;
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds) {
                ResourceLocation loc = toLocation(soundStr);
                Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(loc);
                if (pos != null) {
                    soundEventPos = pos;
                    groupRangeMult = group.rangeMultiplier;
                    break outer;
                }
            }
        }
        boolean soundTriggered = false;
        if (soundEventPos != null) {
            double grpRange = range * groupRangeMult;
            if (mob.level().isRaining() || mob.level().isThundering()) {
                grpRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }
            soundTriggered = mobCenterPos.distanceTo(soundEventPos) <= grpRange;
        }

        boolean pointBlankTriggered = lastPointBlankSoundPos != null && mobCenterPos.distanceTo(lastPointBlankSoundPos) <= effectiveRange;

        boolean hurtTriggered = !(mob instanceof Monster) && lastAttackerPos != null;

        return gunshotTriggered || soundTriggered || pointBlankTriggered || hurtTriggered;
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

    public void onHurt(Vec3 attackerPos) {
        this.lastAttackerPos = attackerPos;
    }

    private void updateNavigation() {
        // 1. Expira el point blank sound si corresponde
        if (lastPointBlankSoundPos != null && System.currentTimeMillis() - lastPointBlankSoundTimestamp > POINT_BLANK_SOUND_EXPIRATION_MS) {
            lastPointBlankSoundPos = null;
            lastPointBlankGunType = null;
        }

        Vec3 currentPos = mob.position();
        Vec3 target = null;
        double effectiveRange = range;
        double effectiveSpeed = speed;  // Inicializamos con la velocidad base
        String effectiveGunType = null;

        GunshotData gunshotData = GunFireListener.getLastGunshotData();
        if (gunshotData != null) {
            effectiveGunType = gunshotData.gunType.name().toLowerCase();
        } else if (lastPointBlankSoundPos != null && lastPointBlankGunType != null) {
            effectiveGunType = lastPointBlankGunType;
        }

        // Ajuste de la velocidad y el rango si se encuentra un tipo de arma o se está en un "point blank"
        if (effectiveGunType != null) {
            double rangeMultiplier;
            double speedMultiplier;
            if (lastPointBlankSoundPos != null) {
                // Se utilizan los multiplicadores de point blank
                rangeMultiplier = SoundConfig.getPointBlankRangeMultiplier(effectiveGunType);
                speedMultiplier = SoundConfig.getPointBlankSpeedMultiplier(effectiveGunType);
            } else {
                // Se utilizan los multiplicadores normales según el tipo de arma
                rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
            }
            effectiveRange = range * rangeMultiplier;
            effectiveSpeed = speed * speedMultiplier;  // Multiplicamos la velocidad
        }

        // Ajuste por condiciones de clima (lluvia/trueno)
        if (mob.level().isRaining() || mob.level().isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        // 2. Reacción a sonidos de grupos prioritarios, con persistencia
        Vec3 prioritySoundPos = null;
        double priorityRange = 0;
        double prioritySpeed = 0;

        // Reacción a los sonidos prioritarios
        for (SoundGroupData priority : SoundConfig.getPriorityGroups()) {
            for (String soundStr : priority.sounds) {
                ResourceLocation loc = toLocation(soundStr);
                Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(loc);
                if (pos != null) {
                    lastPrioritySoundPos = pos;
                    lastPrioritySoundTimestamp = System.currentTimeMillis();
                }
            }
        }

        // Si hay un sonido prioritario reciente guardado
        if (lastPrioritySoundPos != null && (System.currentTimeMillis() - lastPrioritySoundTimestamp) <= PRIORITY_SOUND_PERSISTENCE_MS) {
            prioritySoundPos = lastPrioritySoundPos;
            // Tomamos los multiplicadores del primer grupo que coincida con el sonido almacenado
            for (SoundGroupData priority : SoundConfig.getPriorityGroups()) {
                for (String soundStr : priority.sounds) {
                    if (SoundEventTracker.getLastPlayedPositionForSound(toLocation(soundStr)) != null || lastPrioritySoundPos != null) {
                        priorityRange = priority.rangeMultiplier * range;
                        prioritySpeed = priority.speedMultiplier * speed;  // Aquí multiplicamos la velocidad también
                        if (mob.level().isRaining() || mob.level().isThundering()) {
                            priorityRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
                        }
                        break;
                    }
                }
            }

            if (currentPos.distanceTo(prioritySoundPos) <= priorityRange) {
                if (mob instanceof Monster) {
                    mob.getNavigation().moveTo(prioritySoundPos.x, prioritySoundPos.y, prioritySoundPos.z, prioritySpeed);  // Usamos la velocidad prioritaria
                } else {
                    Vec3 directionAway = currentPos.subtract(prioritySoundPos).normalize();
                    Vec3 fleeTarget = currentPos.add(directionAway.scale(priorityRange));
                    mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, prioritySpeed);  // Usamos la velocidad prioritaria
                }
                return;
            }
        }

        // 3. Reacción a sonidos de grupos normales
        Vec3 soundEventPos = null;
        double groupRangeMult = 1.0;
        double groupSpeedMult = 1.0;

        // Reacción a los sonidos de grupos normales
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds) {
                ResourceLocation loc = toLocation(soundStr);
                Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(loc);
                if (pos != null) {
                    soundEventPos = pos;
                    groupRangeMult = group.rangeMultiplier;
                    groupSpeedMult = group.speedMultiplier;  // Aquí obtenemos el multiplicador de velocidad
                    break outer;
                }
            }
        }

        if (soundEventPos != null) {
            double grpRange = range * groupRangeMult;
            double grpSpeed = speed * groupSpeedMult;  // Aplicamos el multiplicador de velocidad del grupo
            if (mob.level().isRaining() || mob.level().isThundering()) {
                grpRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }

            if (currentPos.distanceTo(soundEventPos) <= grpRange) {
                if (mob instanceof Monster) {
                    mob.getNavigation().moveTo(soundEventPos.x, soundEventPos.y, soundEventPos.z, grpSpeed);  // Usamos la velocidad del grupo
                } else {
                    Vec3 directionAway = currentPos.subtract(soundEventPos).normalize();
                    Vec3 fleeTarget = currentPos.add(directionAway.scale(grpRange));
                    mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, grpSpeed);  // Usamos la velocidad del grupo
                }
                return;
            }
        }

        // 4. Reacción a gunshots o point blank
        if (mob instanceof Monster) {
            if (gunshotData != null && currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
                target = new Vec3(gunshotData.position.x, mob.getY(), gunshotData.position.z);
            } else if (lastPointBlankSoundPos != null && currentPos.distanceTo(lastPointBlankSoundPos) <= effectiveRange) {
                target = new Vec3(lastPointBlankSoundPos.x, mob.getY(), lastPointBlankSoundPos.z);
            }

            if (target != null) {
                mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);  // Usamos la velocidad calculada
                if (currentPos.distanceTo(target) < 1.0) {
                    lastPointBlankSoundPos = null;
                    lastPointBlankGunType = null;
                    stop();
                }
            }
        } else {
            Vec3 dangerPos = null;

            if (gunshotData != null && currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
                dangerPos = gunshotData.position;
            } else if (lastPointBlankSoundPos != null && currentPos.distanceTo(lastPointBlankSoundPos) <= effectiveRange) {
                dangerPos = lastPointBlankSoundPos;
            }

            if (dangerPos != null) {
                Vec3 directionAway = currentPos.subtract(dangerPos).normalize();
                Vec3 fleeTarget = currentPos.add(directionAway.scale(effectiveRange));
                mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, effectiveSpeed);  // Usamos la velocidad calculada
            }
        }
    }



    private ResourceLocation toLocation(String soundStr) {
        if (soundStr.contains(":")) {
            return new ResourceLocation(soundStr);
        }
        return new ResourceLocation("minecraft", soundStr);
    }



    @Mod.EventBusSubscriber(modid = "ezvcsurvival")
    public static class ReactToSoundGoalEventHandler {
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            for (ReactToSoundGoal goal : activeGoals) {
                if (event.getEntity() == goal.mob && !(goal.mob instanceof Monster)) {
                    if (event.getSource().getEntity() != null) {
                        goal.onHurt(event.getSource().getEntity().position());
                    }
                }
            }
        }
    }
}
