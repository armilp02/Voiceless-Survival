package com.armilp.ezvcsurvival.audio.modifier;

public class NoOpAudioModifier implements IAudioModifier {

    public NoOpAudioModifier() {
    }

    @Override
    public double computeModifiedRange(double baseRange) {
        return baseRange;
    }
}
