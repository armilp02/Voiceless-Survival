package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.goals.injector.FollowVoiceGoalInjector;
import com.armilp.ezvcsurvival.goals.injector.RunawayVoiceGoalInjector;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;


@Mod(EZVCSurvival.MOD_ID)
public class EZVCSurvival {
    public static final String MOD_ID = "ezvcsurvival";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EZVCSurvival(IEventBus modEventBus, ModContainer modContainer) {
        // Registro del archivo de configuración
        modContainer.registerConfig(ModConfig.Type.COMMON, VoiceConfig.CONFIG);

        // Registro de eventos
        NeoForge.EVENT_BUS.register(FollowVoiceGoalInjector.class);
        NeoForge.EVENT_BUS.register(RunawayVoiceGoalInjector.class);
        modEventBus.register(this);

        LOGGER.info("EZVCSurvival mod cargado exitosamente.");
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Configuración común para EZVCSurvival.");
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("Configuración del cliente para EZVCSurvival.");
    }
}