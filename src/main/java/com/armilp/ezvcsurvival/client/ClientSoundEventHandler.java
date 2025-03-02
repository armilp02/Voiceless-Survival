package com.armilp.ezvcsurvival.client;

import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.SoundPlayedPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", value = Dist.CLIENT)
public class ClientSoundEventHandler {

    private static final Set<String> POINTBLANK_SOUNDS = new HashSet<>();

    static {
        // PISTOLS
        POINTBLANK_SOUNDS.add("pointblank:glock17"); POINTBLANK_SOUNDS.add("pointblank:m9"); POINTBLANK_SOUNDS.add("pointblank:m1911a1");
        POINTBLANK_SOUNDS.add("pointblank:p30l"); POINTBLANK_SOUNDS.add("pointblank:deserteagle"); POINTBLANK_SOUNDS.add("pointblank:rhino");
        // RIFLE
        POINTBLANK_SOUNDS.add("pointblank:ak12"); POINTBLANK_SOUNDS.add("pointblank:m4a1"); POINTBLANK_SOUNDS.add("pointblank:m4sopmodii");
        POINTBLANK_SOUNDS.add("pointblank:m16a1"); POINTBLANK_SOUNDS.add("pointblank:hk416"); POINTBLANK_SOUNDS.add("pointblank:scarl_unsilenced");
        POINTBLANK_SOUNDS.add("pointblank:xm7_unsilenced"); POINTBLANK_SOUNDS.add("pointblank:g36c"); POINTBLANK_SOUNDS.add("pointblank:aug");
        POINTBLANK_SOUNDS.add("pointblank:g41"); POINTBLANK_SOUNDS.add("pointblank:ak47"); POINTBLANK_SOUNDS.add("pointblank:ak74"); POINTBLANK_SOUNDS.add("pointblank:an94");
        POINTBLANK_SOUNDS.add("pointblank:ar57");POINTBLANK_SOUNDS.add("pointblank:xm29");
        // SMG
        POINTBLANK_SOUNDS.add("pointblank:mp5"); POINTBLANK_SOUNDS.add("pointblank:mp7");
        POINTBLANK_SOUNDS.add("pointblank:ro635"); POINTBLANK_SOUNDS.add("pointblank:ump45_unsilenced");
        POINTBLANK_SOUNDS.add("pointblank:vector"); POINTBLANK_SOUNDS.add("pointblank:p90");
        POINTBLANK_SOUNDS.add("pointblank:m950"); POINTBLANK_SOUNDS.add("pointblank:tmp"); POINTBLANK_SOUNDS.add("pointblank:sl8");
        // SNIPER
        POINTBLANK_SOUNDS.add("pointblank:mk14ebr"); POINTBLANK_SOUNDS.add("pointblank:uar10");
        POINTBLANK_SOUNDS.add("pointblank:g3"); POINTBLANK_SOUNDS.add("pointblank:wa2000");
        POINTBLANK_SOUNDS.add("pointblank:xm3"); POINTBLANK_SOUNDS.add("pointblank:l96a1");
        POINTBLANK_SOUNDS.add("pointblank:ballista"); POINTBLANK_SOUNDS.add("pointblank:gm6lynx");
        // SHOTGUN
        POINTBLANK_SOUNDS.add("pointblank:m590"); POINTBLANK_SOUNDS.add("pointblank:m870");
        POINTBLANK_SOUNDS.add("pointblank:spas12"); POINTBLANK_SOUNDS.add("pointblank:aa12");
        POINTBLANK_SOUNDS.add("pointblank:citoricxs"); POINTBLANK_SOUNDS.add("pointblank:hs12");
        // RPG
        POINTBLANK_SOUNDS.add("pointblank:mgl_shoot"); POINTBLANK_SOUNDS.add("pointblank:launcher"); POINTBLANK_SOUNDS.add("pointblank:at4");
        // MG
        POINTBLANK_SOUNDS.add("pointblank:lamg"); POINTBLANK_SOUNDS.add("pointblank:mk48");
        POINTBLANK_SOUNDS.add("pointblank:m249"); POINTBLANK_SOUNDS.add("pointblank:m134minigun");
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        ResourceLocation soundLocation = event.getSound().getLocation();
        String fullSoundName = soundLocation.getNamespace() + ":" + soundLocation.getPath();

        if (POINTBLANK_SOUNDS.contains(fullSoundName)) {
            EZVCNetwork.INSTANCE.sendToServer(new SoundPlayedPacket(fullSoundName));
        }
    }
}
