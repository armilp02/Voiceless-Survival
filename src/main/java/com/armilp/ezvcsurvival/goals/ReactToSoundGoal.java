package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.events.GunFireListener;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ReactToSoundGoal extends Goal {
    private final MobEntity mob;
    private final double speed;
    private final int range;
    private final List<String> soundStrings;
    private Vector3d lastAttackerPos = null;
    private static final List<ReactToSoundGoal> activeGoals = new CopyOnWriteArrayList<>();

    public ReactToSoundGoal(MobEntity mob, double speed, int range, List<String> soundTypes) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundStrings = soundTypes;
        activeGoals.add(this);
    }

    @Override
    public boolean canUse() {
        List<ResourceLocation> resourceLocations = soundStrings.stream()
                .map(ResourceLocation::new)
                .collect(Collectors.toList());

        Vector3d mobCenterPos = Vector3d.atCenterOf(new BlockPos(mob.position()));
        Vector3d soundPosition = SoundEventTracker.getLastPlayedPositionForAny(resourceLocations);
        Vector3d gunshotPosition = GunFireListener.getLastGunshotPosition();

        boolean soundTriggered = soundPosition != null && soundPosition.distanceTo(mobCenterPos) <= this.range;
        boolean hurtTriggered = !(mob instanceof MonsterEntity) && lastAttackerPos != null;
        boolean gunshotDetected = gunshotPosition != null && gunshotPosition.distanceTo(mobCenterPos) <= this.range;

        return soundTriggered || hurtTriggered || gunshotDetected;
    }

    @Override
    public void start() {
        updateNavigation();
    }

    @Override
    public void stop() {
        activeGoals.remove(this);
    }

    @Override
    public void tick() {
        updateNavigation();
    }

    public void onHurt(Vector3d attackerPos) {
        this.lastAttackerPos = attackerPos;
    }

    private void updateNavigation() {
        Vector3d currentPos = mob.position();
        Vector3d target = null;

        if (mob instanceof MonsterEntity) {
            if (mob.getTarget() instanceof PlayerEntity) {
                return;
            }

            Vector3d gunshotPosition = GunFireListener.getLastGunshotPosition();
            if (gunshotPosition != null) {
                target = new Vector3d(gunshotPosition.x, mob.getY(), gunshotPosition.z);
            }
            List<ResourceLocation> resourceLocations = soundStrings.stream()
                    .map(ResourceLocation::new)
                    .collect(Collectors.toList());
            Vector3d soundPosition = SoundEventTracker.getLastPlayedPositionForAny(resourceLocations);
            if (soundPosition != null) {
                target = new Vector3d(soundPosition.x, mob.getY(), soundPosition.z);
            }
        } else {
            if (lastAttackerPos != null) {
                Vector3d diff = currentPos.subtract(lastAttackerPos);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vector3d(1, 0, 0);
                }
                target = currentPos.add(diff.normalize().scale(range));
                lastAttackerPos = null;
            } else {
                List<ResourceLocation> resourceLocations = soundStrings.stream()
                        .map(ResourceLocation::new)
                        .collect(Collectors.toList());
                Vector3d soundPosition = SoundEventTracker.getLastPlayedPositionForAny(resourceLocations);
                if (soundPosition != null) {
                    Vector3d diff = currentPos.subtract(soundPosition);
                    if (diff.lengthSqr() < 1e-4) {
                        diff = new Vector3d(1, 0, 0);
                    }
                    target = currentPos.add(diff);
                }
            }
            if (target != null) {
                target = new Vector3d(target.x, mob.getY(), target.z);
            }
        }
        if (target != null) {
            mob.getNavigation().moveTo(target.x, target.y, target.z, speed);
        }
    }

    @Mod.EventBusSubscriber(modid = "ezvcsurvival")
    public static class ReactToSoundGoalEventHandler {
        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            for (ReactToSoundGoal goal : activeGoals) {
                if (event.getEntity() == goal.mob && !(goal.mob instanceof MonsterEntity)) {
                    if (event.getSource().getEntity() != null) {
                        goal.onHurt(event.getSource().getEntity().position());
                    }
                }
            }
        }
    }
}
