package com.armilp.ezvcsurvival.client;

import com.armilp.ezvcsurvival.events.SoundEventHandler;
import net.minecraftforge.client.event.sound.PlaySoundEvent;

public class ClientEventRegistration {

    public static void init() {
        PlaySoundEvent.BUS.addListener(SoundEventHandler::onPlaySound);
    }
}