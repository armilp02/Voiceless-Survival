package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.compat.tacz.GunFireListener;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.mojang.logging.LogUtils;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Map;

@Mod(EZVCSurvival.MOD_ID)
public class EZVCSurvival {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EZVCSurvival() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EZVCNetwork.registerPackets();
        if (ModList.get().isLoaded(GunMod.MOD_ID)) {
            MinecraftForge.EVENT_BUS.register(GunFireListener.class);
            CommonAssetsManager assets = CommonAssetsManager.getInstance();
            if (assets != null) {
                for (Map.Entry<ResourceLocation, CommonGunIndex> entry : assets.getAllGuns()) {
                    ResourceLocation gunId = entry.getKey();
                    CommonGunIndex index = entry.getValue();
                    GunFireListener.CommonGunIndexRegistry.registerCommonGunIndex(gunId, index);
                }
            }
       }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SoundConfig.SPEC, "ezvcsurvival/sounds.toml");


        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        EntityVoiceConfig.init();
        GeneralSoundsConfig.init();
        GunfireConfig.init();
        SoundConfig.loadConfigs();
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
