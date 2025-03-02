package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.PointBlankSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", value = Dist.CLIENT)
public class PointBlankSoundEventHandler {

    public static final Set<String> PISTOLS_SOUNDS = new HashSet<>();
    public static final Set<String> SNIPER_SOUNDS = new HashSet<>();
    public static final Set<String> RIFLE_SOUNDS = new HashSet<>();
    public static final Set<String> SMG_SOUNDS = new HashSet<>();
    public static final Set<String> SHOTGUN_SOUNDS = new HashSet<>();
    public static final Set<String> RPG_SOUNDS = new HashSet<>();
    public static final Set<String> MG_SOUNDS = new HashSet<>();

    static {
        // PISTOLS
        PISTOLS_SOUNDS.add("pointblank:glock17");
        PISTOLS_SOUNDS.add("pointblank:m9");
        PISTOLS_SOUNDS.add("pointblank:m1911a1");
        PISTOLS_SOUNDS.add("pointblank:p30l");
        PISTOLS_SOUNDS.add("pointblank:deserteagle");
        PISTOLS_SOUNDS.add("pointblank:rhino");
        // RIFLES
        RIFLE_SOUNDS.add("pointblank:ak12");
        RIFLE_SOUNDS.add("pointblank:m4a1");
        RIFLE_SOUNDS.add("pointblank:m4sopmodii");
        RIFLE_SOUNDS.add("pointblank:m16a1");
        RIFLE_SOUNDS.add("pointblank:hk416");
        RIFLE_SOUNDS.add("pointblank:scarl_unsilenced");
        RIFLE_SOUNDS.add("pointblank:xm7_unsilenced");
        RIFLE_SOUNDS.add("pointblank:g36c");
        RIFLE_SOUNDS.add("pointblank:aug");
        RIFLE_SOUNDS.add("pointblank:g41");
        RIFLE_SOUNDS.add("pointblank:ak47");
        RIFLE_SOUNDS.add("pointblank:ak74");
        RIFLE_SOUNDS.add("pointblank:an94");
        RIFLE_SOUNDS.add("pointblank:ar57");
        RIFLE_SOUNDS.add("pointblank:xm29");
        // SMG
        SMG_SOUNDS.add("pointblank:mp5");
        SMG_SOUNDS.add("pointblank:mp7");
        SMG_SOUNDS.add("pointblank:ro635");
        SMG_SOUNDS.add("pointblank:ump45_unsilenced");
        SMG_SOUNDS.add("pointblank:vector");
        SMG_SOUNDS.add("pointblank:p90");
        SMG_SOUNDS.add("pointblank:m950");
        SMG_SOUNDS.add("pointblank:tmp");
        SMG_SOUNDS.add("pointblank:sl8");
        // SNIPER
        SNIPER_SOUNDS.add("pointblank:mk14ebr");
        SNIPER_SOUNDS.add("pointblank:uar10");
        SNIPER_SOUNDS.add("pointblank:g3");
        SNIPER_SOUNDS.add("pointblank:wa2000");
        SNIPER_SOUNDS.add("pointblank:xm3");
        SNIPER_SOUNDS.add("pointblank:l96a1");
        SNIPER_SOUNDS.add("pointblank:ballista");
        SNIPER_SOUNDS.add("pointblank:gm6lynx");
        // SHOTGUN
        SHOTGUN_SOUNDS.add("pointblank:m590");
        SHOTGUN_SOUNDS.add("pointblank:m870");
        SHOTGUN_SOUNDS.add("pointblank:spas12");
        SHOTGUN_SOUNDS.add("pointblank:aa12");
        SHOTGUN_SOUNDS.add("pointblank:citoricxs");
        SHOTGUN_SOUNDS.add("pointblank:hs12");
        // RPG
        RPG_SOUNDS.add("pointblank:mgl_shoot");
        RPG_SOUNDS.add("pointblank:launcher");
        RPG_SOUNDS.add("pointblank:at4");
        // MG
        MG_SOUNDS.add("pointblank:lamg");
        MG_SOUNDS.add("pointblank:mk48");
        MG_SOUNDS.add("pointblank:m249");
        MG_SOUNDS.add("pointblank:m134minigun");
    }

    private static String getGunTypeForSound(String fullSound) {
        if (PISTOLS_SOUNDS.contains(fullSound)) return "pistol";
        if (SNIPER_SOUNDS.contains(fullSound)) return "sniper";
        if (RIFLE_SOUNDS.contains(fullSound)) return "rifle";
        if (SMG_SOUNDS.contains(fullSound)) return "smg";
        if (SHOTGUN_SOUNDS.contains(fullSound)) return "shotgun";
        if (RPG_SOUNDS.contains(fullSound)) return "rpg";
        if (MG_SOUNDS.contains(fullSound)) return "mg";
        return null;
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        ResourceLocation soundRes = event.getSound().getLocation();
        String fullSound = soundRes.getNamespace() + ":" + soundRes.getPath();

        String gunType = getGunTypeForSound(fullSound);
        if (gunType != null) {
            ReactToSoundGoal.lastPointBlankGunType = gunType;

            double speedMultiplier = SoundConfig.getPointBlankSpeedMultiplier(gunType);
            double rangeMultiplier = SoundConfig.getPointBlankRangeMultiplier(gunType);

            ReactToSoundGoal.lastPointBlankSoundPos = new Vec3(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ());
            ReactToSoundGoal.lastPointBlankSoundTimestamp = System.currentTimeMillis();

            if (Minecraft.getInstance().getConnection() != null) {
                EZVCNetwork.INSTANCE.sendToServer(
                        new PointBlankSoundPacket(
                                soundRes,
                                event.getSound().getX(),
                                event.getSound().getY(),
                                event.getSound().getZ(),
                                speedMultiplier,
                                rangeMultiplier
                        )
                );
            }
        }
    }
}
