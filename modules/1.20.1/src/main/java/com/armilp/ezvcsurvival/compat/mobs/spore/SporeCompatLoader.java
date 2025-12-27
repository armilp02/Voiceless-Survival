package com.armilp.ezvcsurvival.compat.mobs.spore;

import com.Harbinger.Spore.Sentities.BaseEntities.Calamity;
import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.Sentities.BaseEntities.UtilityEntity;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

public class SporeCompatLoader {

    public static void init() {
        if (!ModList.get().isLoaded("spore")) {
            System.out.println("[EZVCSurvival] Spore not found, skipping compatibility.");
            return;
        }

        MinecraftForge.EVENT_BUS.register(new SporeCompatLoader());
        System.out.println("[EZVCSurvival] Spore compatibility active.");
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!EntityVoiceConfig.isEnabled()) return;

        String id = event.getEntity().getType().builtInRegistryHolder().key().location().toString();
        if (!id.startsWith("spore:")) return;

        EntityVoiceConfig.EntityConfig cfg = EntityVoiceConfig.getOrCreate(id);
        if (!cfg.enabled) return;

        try {
            Goal voiceGoal = null;

            if (event.getEntity() instanceof Infected infected) {
                voiceGoal = new SporeVoiceTargetGoal(
                        infected,
                        cfg.speed,
                        (int) cfg.range,
                        cfg.threshold,
                        10000L
                );
                infected.goalSelector.addGoal(1, voiceGoal);
            } else if (event.getEntity() instanceof Calamity calamity) {
                voiceGoal = new SporeVoiceTargetGoal(
                        calamity,
                        cfg.speed,
                        (int) cfg.range,
                        cfg.threshold,
                        10000L
                );
                calamity.goalSelector.addGoal(1, voiceGoal);
            } else if (event.getEntity() instanceof UtilityEntity utilityEntity) {
                voiceGoal = new SporeVoiceTargetGoal(
                        utilityEntity,
                        cfg.speed,
                        (int) cfg.range,
                        cfg.threshold,
                        10000L
                );
                utilityEntity.goalSelector.addGoal(1, voiceGoal);
            }

            if (voiceGoal != null) {
                System.out.println("[EZVCSurvival] Added SporeVoiceTargetGoal to " + id);
            }

        } catch (Exception e) {
            System.err.println("[EZVCSurvival] Failed to add goal to Spore entity: " + e);
            e.printStackTrace();
        }
    }
}