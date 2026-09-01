package com.armilp.ezvcsurvival.client;

import com.armilp.ezvcsurvival.voicechat.client.DbMeterOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EZClient {

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("db_meter", DbMeterOverlay.INSTANCE);
    }
}
