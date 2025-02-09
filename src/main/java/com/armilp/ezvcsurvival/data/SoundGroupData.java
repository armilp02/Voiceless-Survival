package com.armilp.ezvcsurvival.data;

import java.util.List;

public class SoundGroupData {
    private final String groupName;
    private final List<String> sounds;
    private final double speedMultiplier;
    private final double rangeMultiplier;

    public SoundGroupData(String groupName, List<String> sounds, double speedMultiplier, double rangeMultiplier) {
        this.groupName = groupName;
        this.sounds = sounds;
        this.speedMultiplier = speedMultiplier;
        this.rangeMultiplier = rangeMultiplier;
    }

    public String getGroupName() {
        return groupName;
    }

    public List<String> getSounds() {
        return sounds;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public double getRangeMultiplier() {
        return rangeMultiplier;
    }
}
