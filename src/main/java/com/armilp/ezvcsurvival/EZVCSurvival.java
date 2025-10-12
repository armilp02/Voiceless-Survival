package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.events.MobGoalInjector;
import com.armilp.ezvcsurvival.events.SoundEventHandler;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(EZVCSurvival.MOD_ID)
public class EZVCSurvival {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EZVCSurvival(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, SoundConfig.SPEC, "ezvcsurvival/sounds.toml");

        modEventBus.register(this);
        modEventBus.register(EZVCNetwork.class);

        NeoForge.EVENT_BUS.register(SoundEventHandler.class);
        NeoForge.EVENT_BUS.register(MobGoalInjector.class);
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        SoundConfig.loadConfigs();
        LOGGER.info("SoundConfig loaded");
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {

    }
}