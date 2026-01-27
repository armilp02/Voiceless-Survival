package com.armilp.ezvcsurvival.mixins;

import net.minecraft.entity.ai.EntityAITasks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(EntityAITasks.class)
public interface GoalSelectorAccessor {
    @Accessor("taskEntries")
    Set<EntityAITasks.EntityAITaskEntry> getTaskEntries();
}