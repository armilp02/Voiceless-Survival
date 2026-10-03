package com.armilp.ezvcsurvival.voicechat;

import net.minecraftforge.fml.ModList;

public class VoiceModCheck {

    private VoiceModCheck(){
    }

    public static boolean hasSimpleVoiceChat(){
        return ModList.get().isLoaded("voicechat");
    }

    public static boolean hasPlasmoVoice(){
        return ModList.get().isLoaded("plasmovoice");
    }

    public static void verify(){
        if (!hasSimpleVoiceChat() && !hasPlasmoVoice()){
            throw new IllegalStateException("Voiceless Survival requires either Simple Voice Chat or Plasmo Voice to be installed.");
        }
    }
}
