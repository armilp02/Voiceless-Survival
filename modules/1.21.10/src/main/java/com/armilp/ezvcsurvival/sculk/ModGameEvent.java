package com.armilp.ezvcsurvival.sculk;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModGameEvent {

    public static final DeferredRegister<GameEvent> GAME_EVENTS =
            DeferredRegister.create(Registries.GAME_EVENT, EZVCSurvival.MOD_ID);

    public static final DeferredHolder<GameEvent, GameEvent> VOICE_TALK =
            GAME_EVENTS.register("voice_talk", () -> new GameEvent(16));

}