package com.armilp.ezvcsurvival.compat;

import com.armilp.ezvcsurvival.data.GunshotData;
import com.tac.guns.common.Gun;
import com.tac.guns.util.GunModifierHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TACFireListener {

    private static final List<GunshotData> gunshotPositions = new CopyOnWriteArrayList<>();
    private static final long EXPIRATION_TIME_MS = 5000L;
    private static final boolean TACZ_LOADED = ModList.get().isLoaded("tac");

    static {
        if (TACZ_LOADED) {
            MinecraftForge.EVENT_BUS.register(TACFireListener.class);
        }
    }

    @SubscribeEvent
    public static void onGunFire(Event event) {
        if (!TACZ_LOADED) {
            return;
        }

        try {
            Class<?> gunFireEventClass = Class.forName("com.tac.guns.event.GunFireEvent");
            if (!gunFireEventClass.isInstance(event)) {
                return;
            }

            Method getPlayerMethod = gunFireEventClass.getMethod("getPlayer");
            Object playerObj = getPlayerMethod.invoke(event);

            if (!(playerObj instanceof PlayerEntity)) {
                return;
            }

            PlayerEntity player = (PlayerEntity) playerObj;
            ItemStack itemInMainHand = player.getItemInHand(Hand.MAIN_HAND);
            ItemStack itemInOffHand = player.getItemInHand(Hand.OFF_HAND);

            if (!Gun.hasAmmo(itemInMainHand) && !Gun.hasAmmo(itemInOffHand)) {
                return;
            }

            ItemStack weapon = Gun.hasAmmo(itemInMainHand) ? itemInMainHand : itemInOffHand;
            boolean isSilenced = GunModifierHelper.isSilencedFire(weapon);

            if (isSilenced) {
                return;
            }

            String gunType = extractGunType(weapon);
            Vector3d shooterPos = new Vector3d(player.getX(), player.getY(), player.getZ());
            gunshotPositions.add(new GunshotData(shooterPos, System.currentTimeMillis(), gunType));

        } catch (Exception e) {
            System.err.println("[GunFireListener] Error al procesar GunFireEvent:");
            e.printStackTrace();
        }
    }

    private static String extractGunType(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            return "";
        }

        try {
            String itemId = weapon.getItem().getRegistryName().toString();

            if (itemId.contains(":")) {
                String[] parts = itemId.split(":");
                return parts.length > 1 ? parts[1] : itemId;
            }

            return itemId;
        } catch (Exception e) {
            return "";
        }
    }

    public static GunshotData getLastGunshot() {
        long currentTime = System.currentTimeMillis();
        gunshotPositions.removeIf(record -> currentTime - record.timestamp > EXPIRATION_TIME_MS);

        if (gunshotPositions.isEmpty()) {
            return null;
        }

        return gunshotPositions.get(gunshotPositions.size() - 1);
    }
}