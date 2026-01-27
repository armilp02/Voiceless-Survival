package com.armilp.ezvcsurvival.proxy;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        EZVCSurvival.LOGGER.info("CommonProxy - PreInit");
        // NO inicializar network aquí
    }

    public void init(FMLInitializationEvent event) {
        EZVCSurvival.LOGGER.info("CommonProxy - Init");

        // Mover la inicialización del network AQUÍ
        EZVCNetwork.init();
    }

    public void postInit(FMLPostInitializationEvent event) {
        EZVCSurvival.LOGGER.info("CommonProxy - PostInit");
    }
}