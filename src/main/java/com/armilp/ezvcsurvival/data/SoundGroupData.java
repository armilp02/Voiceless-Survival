package com.armilp.ezvcsurvival.data;

import java.util.List;

public class SoundGroupData {
    public final String groupName;
    public final List<String> sounds;
    public final double speedMultiplier;
    public final double rangeMultiplier;

    public SoundGroupData(String groupName, List<String> sounds, double speedMultiplier, double rangeMultiplier) {
        this.groupName = groupName;
        this.sounds = sounds;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
    }
}
