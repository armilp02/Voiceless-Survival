package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.FollowVoiceConfig;
import com.armilp.ezvcsurvival.config.FollowVoiceGoalInjector;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EZVCSurvival.MOD_ID)
public class EZVCSurvival {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EZVCSurvival() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FollowVoiceConfig.CONFIG);

        // Registra eventos
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        // Registro en Forge Event Bus
        MinecraftForge.EVENT_BUS.register(this);

        // Carga configuración
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Add items to the creative tab logic
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Client setup logic
        }
    }
}
