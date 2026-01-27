package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.client.ClientEventRegistration;
import com.armilp.ezvcsurvival.commands.OpenConfigCommand;
import com.armilp.ezvcsurvival.commands.ReloadConfigCommand;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.events.MobGoalInjector;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.sculk.ModGameEvent;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(EZVCSurvival.MOD_ID)
public class EZVCSurvival {
    public static final String MOD_ID = "ezvcsurvival";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EZVCSurvival(FMLJavaModLoadingContext context) {
        var modEventBus = context.getModBusGroup();

        EZVCNetwork.registerPackets();

        ModGameEvent.GAME_EVENTS.register(modEventBus);
        SoundConfig.registerConfigListeners(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, VoiceConfig.CONFIG, "ezvcsurvival/voices.toml");
        context.registerConfig(ModConfig.Type.COMMON, SoundConfig.SPEC, "ezvcsurvival/sounds.toml");

        FMLCommonSetupEvent.getBus(modEventBus).addListener(this::commonSetup);

        RegisterCommandsEvent.BUS.addListener(this::onRegisterCommands);
        EntityJoinLevelEvent.BUS.addListener(MobGoalInjector::onEntityJoin);

        if (FMLEnvironment.dist.isClient()) {
            ClientEventRegistration.init();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        EntityVoiceConfig.init();
        GeneralSoundsConfig.init();
        SoundConfig.loadConfigs();

        event.enqueueWork(ModGameEvent::setupFrequency);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        OpenConfigCommand.onRegisterCommands(event);
        ReloadConfigCommand.onRegisterCommands(event);
    }
}