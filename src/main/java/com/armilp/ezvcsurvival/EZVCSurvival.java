package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.events.SoundEventHandler;
import com.armilp.ezvcsurvival.goals.injector.ReactToSoundGoalInjector;
import com.armilp.ezvcsurvival.goals.ReactToSoundGoal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(EZVCSurvival.MOD_ID)
public class EZVCSurvival {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EZVCSurvival(IEventBus modEventBus, ModContainer modContainer) {
        // Registrar las configuraciones
        modContainer.registerConfig(ModConfig.Type.COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, SoundConfig.SPEC, "ezvcsurvival/sounds.toml");

        // Registrar esta clase para recibir eventos de ciclo de vida
        modEventBus.register(this);

        // Registrar los manejadores de eventos en el bus de NeoForge
        NeoForge.EVENT_BUS.register(new SoundEventHandler());
        NeoForge.EVENT_BUS.register(new ReactToSoundGoalInjector());
        NeoForge.EVENT_BUS.register(new ReactToSoundGoal.ReactToSoundGoalEventHandler());
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        SoundConfig.loadConfigs();
        LOGGER.info("SoundConfig cargado correctamente");
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {

    }
}