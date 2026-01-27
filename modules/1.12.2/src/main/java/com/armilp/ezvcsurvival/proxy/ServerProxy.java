package com.armilp.ezvcsurvival.proxy;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Proxy para el lado del servidor
 * Aquí va todo el código que SOLO debe ejecutarse en el servidor dedicado
 */
public class ServerProxy extends CommonProxy {
    
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        EZVCSurvival.LOGGER.info("ServerProxy - PreInit");
        
        // Inicialización específica del servidor
    }
    
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        EZVCSurvival.LOGGER.info("ServerProxy - Init");
    }
    
    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        EZVCSurvival.LOGGER.info("ServerProxy - PostInit");
    }
}