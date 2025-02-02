package com.armilp.ezvcsurvival.events;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

@Mod("ezvcsurvival") // Solo el modid aquí
public class SoundEventHandler {

    public SoundEventHandler() {
        // Registra eventos en el bus de Forge
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        // Verifica que el sonido sea una instancia de SimpleSoundInstance (que tiene posición)
        if (!(event.getSound() instanceof SimpleSoundInstance sound)) {
            return;
        }

        // Extrae la posición del sonido
        Vec3 position = new Vec3(sound.getX(), sound.getY(), sound.getZ());

        // Obtiene el identificador del sonido
        ResourceLocation id = sound.getLocation();
        SoundEventTracker.registerSound(id, position);
    }
}
