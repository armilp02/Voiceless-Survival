package com.armilp.ezvcsurvival.data;

import java.util.List;

public record SoundGroupData(String groupName, List<String> sounds, double speedMultiplier, double rangeMultiplier) {
}
