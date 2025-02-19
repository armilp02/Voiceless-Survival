package com.armilp.ezvcsurvival.events;

import com.armilp.ezvcsurvival.commands.SoundEffectCommand;
import com.armilp.ezvcsurvival.data.GunshotData;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = "ezvcsurvival", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GunFireListener {

    private static final List<GunshotData> gunshotPositions = new CopyOnWriteArrayList<>();
    private static final long EXPIRATION_TIME_MS = 5000;
    private static final boolean TACZ_LOADED = ModList.get().isLoaded("tacz");

    // Conjunto de identificadores conocidos de silenciadores de otros mods
    private static final Set<String> KNOWN_SILENCERS = new HashSet<>(Arrays.asList(
            "gucci_attachments:muzzle_s_gsx",
            "gucci_attachments:muzzle_s_fd197",
            "gucci_attachments:muzzle_s_gogol9",
            "gucci_attachemtns:muzzle_s_fss5",
            "gucci_attachments:muzzle_s_widemouth",
            "gucci_attachments:muzzle_s_c784",
            "gucci_attachments:muzzle_s_osprey"
    ));

    static {
        MinecraftForge.EVENT_BUS.register(GunFireListener.class);
    }

    @SubscribeEvent
    public static void onGunFire(Event event) {
        if (TACZ_LOADED && event instanceof GunFireEvent gunFireEvent) {
            ItemStack gunStack = gunFireEvent.getGunItemStack();
            IGun gun = IGun.getIGunOrNull(gunStack);
            boolean hasSilencer = false;

            if (gun != null) {
                for (AttachmentType type : AttachmentType.values()) {
                    ResourceLocation attachmentId = gun.getAttachmentId(gunStack, type);
                    String attachmentStr = attachmentId.toString().toLowerCase();
                    // Comprueba si contiene "silencer" o "silenced"
                    if (attachmentStr.contains("silencer") || attachmentStr.contains("silenced")) {
                        hasSilencer = true;
                        break;
                    }
                    if (KNOWN_SILENCERS.contains(attachmentStr)) {
                        hasSilencer = true;
                        break;
                    }
                }
            }

            if (hasSilencer) {
                return;
            }

            Vec3 shooterPos = gunFireEvent.getShooter().position();

            GunTabType gunType = GunTabType.PISTOL;
            if (gun != null) {
                ResourceLocation gunId = gun.getGunId(gunStack);
                String gunIdStr = gunId.toString().toLowerCase();
                if (gunIdStr.contains("sniper")) {
                    gunType = GunTabType.SNIPER;
                } else if (gunIdStr.contains("rifle")) {
                    gunType = GunTabType.RIFLE;
                } else if (gunIdStr.contains("shotgun")) {
                    gunType = GunTabType.SHOTGUN;
                } else if (gunIdStr.contains("smg")) {
                    gunType = GunTabType.SMG;
                } else if (gunIdStr.contains("rpg")) {
                    gunType = GunTabType.RPG;
                } else if (gunIdStr.contains("mg")) {
                    gunType = GunTabType.MG;
                }
            }

            gunshotPositions.add(new GunshotData(shooterPos, System.currentTimeMillis(), gunType));

            if (gunFireEvent.getShooter() instanceof ServerPlayer serverPlayer) {
                SoundEffectCommand.applyEffect(serverPlayer);
            }
        }
    }

    public static GunshotData getLastGunshotData() {
        long currentTime = System.currentTimeMillis();
        gunshotPositions.removeIf(record -> currentTime - record.timestamp > EXPIRATION_TIME_MS);
        return gunshotPositions.isEmpty() ? null : gunshotPositions.get(gunshotPositions.size() - 1);
    }
}
