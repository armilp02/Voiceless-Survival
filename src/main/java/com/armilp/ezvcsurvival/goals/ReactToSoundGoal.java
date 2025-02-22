package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReactToSoundGoal extends net.minecraft.world.entity.ai.goal.Goal {
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

        // Se obtiene la última posición de algún sonido reproducido de los grupos definidos.
        Vec3 soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds) {
                ResourceLocation res = getResourceLocation(soundStr);
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
            soundTriggered = mobCenterPos.distanceTo(soundEventPos) <= groupEffectiveRange;
        }

        boolean hurtTriggered = !(mob instanceof Monster) && lastAttackerPos != null;

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

    public void onHurt(Vec3 attackerPos) {
        this.lastAttackerPos = attackerPos;
    }

    private void updateNavigation() {
        Vec3 currentPos = mob.position();
        Vec3 target = null;
        double effectiveRange = range;
        double effectiveSpeed = speed;

        // Se obtiene la última posición de algún sonido reproducido de los grupos definidos.
        Vec3 soundEventPos = null;
        double groupSpeedMult = 1.0;
        double groupRangeMult = 1.0;
        outer:
        for (SoundGroupData group : soundGroups) {
            for (String soundStr : group.sounds) {
                ResourceLocation res = getResourceLocation(soundStr);
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

        if (mob instanceof Monster) {
            if (mob.getTarget() instanceof Player) {
                return;
            }
            if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
                target = new Vec3(soundEventPos.x, mob.getY(), soundEventPos.z);
            }
        } else {
            if (lastAttackerPos != null) {
                Vec3 diff = currentPos.subtract(lastAttackerPos);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vec3(1, 0, 0);
                }
                target = currentPos.add(diff.normalize().scale(effectiveRange));
                lastAttackerPos = null;
            } else if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
                Vec3 diff = currentPos.subtract(soundEventPos);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vec3(1, 0, 0);
                }
                target = currentPos.add(diff.normalize());
            }
        }

        if (target != null) {
            target = new Vec3(target.x, mob.getY(), target.z);
            mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
        }
    }

    private ResourceLocation getResourceLocation(String soundStr) {
        if (!soundStr.contains(":")) {
            soundStr = "minecraft:" + soundStr;
        }
        ResourceLocation res = ResourceLocation.tryParse(soundStr);
        return Objects.requireNonNull(res, "Invalid ResourceLocation: " + soundStr);
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
