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

    public ReactToSoundGoal(Mob mob, double speed, int range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        activeGoals.add(this);
    }

    @Override
    public boolean canUse() {
        Vec3 mobCenterPos = mob.position();
        double effectiveRange = range;
        double effectiveSpeed = speed;
        String effectiveGunType = null;

        GunshotData gunshotData = GunFireListener.getLastGunshotData();
        if (gunshotData != null) {
            effectiveGunType = gunshotData.gunType.name().toLowerCase();
        }

        if (effectiveGunType != null) {
            double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
            double speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
            effectiveRange = (int) (range * rangeMultiplier);
            effectiveSpeed = speed * speedMultiplier;
        }
        if (mob.level.isRaining() || mob.level.isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        boolean gunshotTriggered = gunshotData != null && mobCenterPos.distanceTo(gunshotData.position) <= effectiveRange;

        Vec3 soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds) {
                ResourceLocation res = new ResourceLocation(soundStr);
                Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(res);
                if (pos != null) {
                    soundEventPos = pos;
                    groupSpeedMult = group.speedMultiplier;
                    groupRangeMult = group.rangeMultiplier;
                    break outer;
                }
            }
        }

        boolean soundTriggered = false;
        if (soundEventPos != null) {
            double groupEffectiveRange = range * groupRangeMult;
            if (mob.level.isRaining() || mob.level.isThundering()) {
                groupEffectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }
            soundTriggered = mobCenterPos.distanceTo(soundEventPos) <= groupEffectiveRange;
        }

        boolean hurtTriggered = !(mob instanceof Monster) && lastAttackerPos != null;

        return gunshotTriggered || soundTriggered || hurtTriggered;
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
        Vec3 currentPos = mob.position();
        Vec3 target = null;
        double effectiveRange = range;
        double effectiveSpeed = speed;
        String effectiveGunType = null;

        GunshotData gunshotData = GunFireListener.getLastGunshotData();
        if (gunshotData != null) {
            effectiveGunType = gunshotData.gunType.name().toLowerCase();
        }

        if (effectiveGunType != null) {
            double rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
            double speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
            effectiveRange = (int) (range * rangeMultiplier);
            effectiveSpeed = speed * speedMultiplier;
        }

        if (mob.level.isRaining() || mob.level.isThundering()) {
            effectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
        }

        Vec3 soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds) {
                ResourceLocation res = new ResourceLocation(soundStr);
                Vec3 pos = SoundEventTracker.getLastPlayedPositionForSound(res);
                if (pos != null) {
                    soundEventPos = pos;
                    groupSpeedMult = group.speedMultiplier;
                    groupRangeMult = group.rangeMultiplier;
                    break outer;
                }
            }
        }
        if (soundEventPos != null) {
            effectiveRange = (int) (range * groupRangeMult);
            effectiveSpeed = speed * groupSpeedMult;
        }

        if (gunshotData != null && currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
            target = new Vec3(gunshotData.position.x, mob.getY(), gunshotData.position.z);
        } else if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
            target = new Vec3(soundEventPos.x, mob.getY(), soundEventPos.z);
        }

        if (target != null) {
            target = new Vec3(target.x, mob.getY(), target.z);
            mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
        }
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
