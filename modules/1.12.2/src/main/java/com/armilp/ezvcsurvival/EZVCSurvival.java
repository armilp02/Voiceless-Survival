package com.armilp.ezvcsurvival;

import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.SoundConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.proxy.CommonProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = EZVCSurvival.MOD_ID,
        name = EZVCSurvival.MOD_NAME,
        version = EZVCSurvival.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:voicechat@[1.12.2-2.6.0,)"
)
public class EZVCSurvival {
    public static final String MOD_ID = "ezvcsurvival";
    public static final String MOD_NAME = "Voiceless Survival";
    public static final String VERSION = "2.0.0";

    // Rutas a los proxies
    public static final String CLIENT_PROXY = "com.armilp.ezvcsurvival.proxy.ClientProxy";
    public static final String SERVER_PROXY = "com.armilp.ezvcsurvival.proxy.ServerProxy";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Mod.Instance(MOD_ID)
    public static EZVCSurvival instance;

    // Sistema de Proxies - Forge selecciona automáticamente el correcto
    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = SERVER_PROXY)
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("========================================");
        LOGGER.info("EZVCSurvival Pre-Initialization Starting");
        LOGGER.info("MOD_ID: " + MOD_ID);
        LOGGER.info("========================================");

        try {
            // Inicializar configs
            LOGGER.info("[1/2] Initializing configs...");
            VoiceConfig.init(event.getModConfigurationDirectory());
            SoundConfig.init(event.getModConfigurationDirectory());
            EntityVoiceConfig.init();
            GeneralSoundsConfig.init();
            LOGGER.info("[1/2] Configs initialized successfully");

            // Llamar al proxy (aquí se inicializa el network)
            LOGGER.info("[2/2] Calling proxy.preInit()...");
            proxy.preInit(event);
            LOGGER.info("[2/2] Proxy preInit completed");

        } catch (Exception e) {
            LOGGER.error("========================================");
            LOGGER.error("FATAL ERROR during pre-initialization!");
            LOGGER.error("========================================");
            LOGGER.error("Error details:", e);
            throw new RuntimeException("Pre-initialization failed", e);
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("EZVCSurvival Initialization");

        try {
            // Registrar eventos
            MinecraftForge.EVENT_BUS.register(this);

            // Cargar configs
            SoundConfig.loadConfigs();

            // Llamar al proxy
            proxy.init(event);

            LOGGER.info("Event bus registered and configs loaded");
        } catch (Exception e) {
            LOGGER.error("Error during initialization!", e);
            throw new RuntimeException("Initialization failed", e);
        }
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("EZVCSurvival Post-Initialization");

        try {
            // Llamar al proxy
            proxy.postInit(event);

            LOGGER.info("EZVCSurvival Post-Initialization Complete");
        } catch (Exception e) {
            LOGGER.error("Error during post-initialization!", e);
            throw new RuntimeException("Post-initialization failed", e);
        }
    }
}