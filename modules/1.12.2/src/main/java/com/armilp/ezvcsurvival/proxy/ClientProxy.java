package com.armilp.ezvcsurvival.proxy;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Proxy para el lado del cliente
 * Aquí va todo el código que SOLO debe ejecutarse en el cliente
 */
public class ClientProxy extends CommonProxy {
    
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        EZVCSurvival.LOGGER.info("ClientProxy - PreInit");
        
        // Aquí puedes registrar renders, keybindings, etc.
    }
    
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        EZVCSurvival.LOGGER.info("ClientProxy - Init");
        
        // Inicialización específica del cliente
    }
    
    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
        EZVCSurvival.LOGGER.info("ClientProxy - PostInit");
    }
}