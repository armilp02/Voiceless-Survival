package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.*;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.logging.Logger;

@Mod(EZVCSurvival.MOD_ID)
public class EZVCSurvival {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    public EZVCSurvival() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EZVCNetwork.registerPackets();

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
}
