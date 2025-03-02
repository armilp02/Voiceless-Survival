package com.armilp.ezvcsurvival.goals;

import com.armilp.ezvcsurvival.commands.SoundEffectCommand;
import com.armilp.ezvcsurvival.data.GunshotData;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.armilp.ezvcsurvival.events.GunFireListener;
import com.armilp.ezvcsurvival.events.SoundEventTracker;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.PointBlankSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
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

    public ReactToSoundGoal(Mob mob, double speed, int range, List<SoundGroupData> soundGroups) {
        this.mob = mob;
        this.speed = speed;
        this.range = range;
        this.soundGroups = soundGroups;
        activeGoals.add(this);
    }

    @Override
    public boolean canUse() {
        if (lastPointBlankSoundPos != null &&
                System.currentTimeMillis() - lastPointBlankSoundTimestamp > POINT_BLANK_SOUND_EXPIRATION_MS) {
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
            double rangeMultiplier, speedMultiplier;
            if (lastPointBlankSoundPos != null) {
                rangeMultiplier = SoundConfig.getPointBlankRangeMultiplier(effectiveGunType);
                speedMultiplier = SoundConfig.getPointBlankSpeedMultiplier(effectiveGunType);
            } else {
                rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
            }
            effectiveRange = (int) (range * rangeMultiplier);
            effectiveSpeed = speed * speedMultiplier;
        }
        if (mob.level().isRaining() || mob.level().isThundering()) {
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
            if (mob.level().isRaining() || mob.level().isThundering()) {
                groupEffectiveRange *= SoundConfig.THUNDER_RANGE_MULTIPLIER.get();
            }
            soundTriggered = mobCenterPos.distanceTo(soundEventPos) <= groupEffectiveRange;
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
        if (lastPointBlankSoundPos != null &&
                System.currentTimeMillis() - lastPointBlankSoundTimestamp > POINT_BLANK_SOUND_EXPIRATION_MS) {
            lastPointBlankSoundPos = null;
            lastPointBlankGunType = null;
        }

        Vec3 currentPos = mob.position();
        Vec3 target = null;
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
            double rangeMultiplier, speedMultiplier;
            if (lastPointBlankSoundPos != null) {
                rangeMultiplier = SoundConfig.getPointBlankRangeMultiplier(effectiveGunType);
                speedMultiplier = SoundConfig.getPointBlankSpeedMultiplier(effectiveGunType);
            } else {
                rangeMultiplier = SoundConfig.getRangeMultiplier(effectiveGunType);
                speedMultiplier = SoundConfig.getSpeedMultiplier(effectiveGunType);
            }
            effectiveRange = (int) (range * rangeMultiplier);
            effectiveSpeed = speed * speedMultiplier;
        }

        if (mob.level().isRaining() || mob.level().isThundering()) {
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

        if (mob instanceof Monster) {
            if (mob.getTarget() instanceof Player) {
                if (mob.getTarget() instanceof ServerPlayer) {
                    SoundEffectCommand.applyEffect((ServerPlayer) mob.getTarget());
                }
                return;
            }
            if (gunshotData != null && currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
                target = new Vec3(gunshotData.position.x, mob.getY(), gunshotData.position.z);
            } else if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
                target = new Vec3(soundEventPos.x, mob.getY(), soundEventPos.z);
            } else if (lastPointBlankSoundPos != null && currentPos.distanceTo(lastPointBlankSoundPos) <= effectiveRange) {
                double offsetX = (Math.random() - 0.5) * 2;
                double offsetZ = (Math.random() - 0.5) * 2;
                target = new Vec3(lastPointBlankSoundPos.x + offsetX, mob.getY(), lastPointBlankSoundPos.z + offsetZ);
            }
        } else {
            if (lastAttackerPos != null) {
                Vec3 diff = currentPos.subtract(lastAttackerPos);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vec3(1, 0, 0);
                }
                target = currentPos.add(diff.normalize().scale(effectiveRange));
                lastAttackerPos = null;
            } else if (gunshotData != null && currentPos.distanceTo(gunshotData.position) <= effectiveRange) {
                Vec3 diff = currentPos.subtract(gunshotData.position);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vec3(1, 0, 0);
                }
                target = currentPos.add(diff.normalize().scale(effectiveRange));
            } else if (soundEventPos != null && currentPos.distanceTo(soundEventPos) <= effectiveRange) {
                Vec3 diff = currentPos.subtract(soundEventPos);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vec3(1, 0, 0);
                }
                target = currentPos.add(diff.normalize().scale(effectiveRange));
            } else if (lastPointBlankSoundPos != null && currentPos.distanceTo(lastPointBlankSoundPos) <= effectiveRange) {
                Vec3 diff = currentPos.subtract(lastPointBlankSoundPos);
                if (diff.lengthSqr() < 1e-4) {
                    diff = new Vec3(1, 0, 0);
                }
                double offsetX = (Math.random() - 0.5) * 2;
                double offsetZ = (Math.random() - 0.5) * 2;
                target = currentPos.add(diff.normalize().scale(effectiveRange)).add(new Vec3(offsetX, 0, offsetZ));
            }
        }

        if (target != null) {
            target = new Vec3(target.x, mob.getY(), target.z);
            mob.getNavigation().moveTo(target.x, target.y, target.z, effectiveSpeed);
            if (currentPos.distanceTo(target) < 1.0) {
                lastPointBlankSoundPos = null;
                lastPointBlankGunType = null;
                stop();
            }
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
