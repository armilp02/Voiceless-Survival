package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.events.GunFireListener;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
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
    private final List<String> soundStrings;

    // Stores the attacker's position when the mob takes damage.
    private Vec3 lastAttackerPos = null;

    // Static list to keep track of active ReactToSoundGoal instances.
    private static final List<ReactToSoundGoal> activeGoals = new CopyOnWriteArrayList<>();

    public ReactToSoundGoal(Mob mob, double speed, int range, List<String> soundTypes) {
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
                .toList();

        Vec3 mobCenterPos = Vec3.atCenterOf(BlockPos.containing(mob.position()));
        Vec3 soundPosition = SoundEventTracker.getLastPlayedPositionForAny(resourceLocations);
        Vec3 gunshotPosition = GunFireListener.getLastGunshotPosition();

        // The goal activates if the sound was played within range...
        boolean soundTriggered = soundPosition != null && soundPosition.distanceTo(mobCenterPos) <= this.range;
        // ...or if the mob (which is not a monster) was recently hit.
        boolean hurtTriggered = !(mob instanceof Monster) && lastAttackerPos != null;
        // ...or if a gunshot was detected nearby.
        boolean gunshotDetected = gunshotPosition != null && gunshotPosition.distanceTo(mobCenterPos) <= this.range;

        return soundTriggered || hurtTriggered || gunshotDetected;
    }

    @Override
    public void start() {
        updateNavigation();
    }

    @Override
    public void stop() {
        // Remove this instance from the active goals list.
        activeGoals.remove(this);
    }

    @Override
    public void tick() {
        updateNavigation();
    }

    public void onHurt(Vec3 attackerPos) {
        this.lastAttackerPos = attackerPos;
    }

    private void updateNavigation() {
        Vec3 currentPos = mob.position();
        Vec3 target = null;


        if (mob instanceof Monster) {
            // If the hostile mob already has a target that is a player, cancel the reaction.
            if (mob.getTarget() != null && mob.getTarget() instanceof Player) {
                return;
            }

            Vec3 gunshotPosition = GunFireListener.getLastGunshotPosition();
            if (gunshotPosition != null) {
                target = new Vec3(gunshotPosition.x, mob.getY(), gunshotPosition.z);
            }
            // Hostile mobs: move towards the sound.
            List<ResourceLocation> resourceLocations = soundStrings.stream()
                    .map(ResourceLocation::new)
                    .toList();
            Vec3 soundPosition = SoundEventTracker.getLastPlayedPositionForAny(resourceLocations);
            if (soundPosition != null) {
                target = new Vec3(soundPosition.x, mob.getY(), soundPosition.z);
            }
        } else {
            // Non-hostile mobs: if they have been hit, run away from the attacker.
            if (lastAttackerPos != null) {
                Vec3 diff = currentPos.subtract(lastAttackerPos);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vec3(1, 0, 0);
                }
                // Normalize and scale the vector so that the mob moves away significantly (using 'range').
                target = currentPos.add(diff.normalize().scale(range));
                // Reset the attacker's position once the attack has been processed.
                lastAttackerPos = null;
            } else {
                // If no damage was received, move away from the sound.
                List<ResourceLocation> resourceLocations = soundStrings.stream()
                        .map(ResourceLocation::new)
                        .toList();
                Vec3 soundPosition = SoundEventTracker.getLastPlayedPositionForAny(resourceLocations);
                if (soundPosition != null) {
                    Vec3 diff = currentPos.subtract(soundPosition);
                    if (diff.lengthSqr() < 1e-4) {
                        diff = new Vec3(1, 0, 0);
                    }
                    target = currentPos.add(diff);
                }
            }
            // Maintain the current height to avoid issues with pathfinding.
            if (target != null) {
                target = new Vec3(target.x, mob.getY(), target.z);
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
                if (event.getEntity() == goal.mob && !(goal.mob instanceof Monster)) {
                    if (event.getSource().getEntity() != null) {
                        goal.onHurt(event.getSource().getEntity().position());
                    }
                }
            }
        }
    }
}
