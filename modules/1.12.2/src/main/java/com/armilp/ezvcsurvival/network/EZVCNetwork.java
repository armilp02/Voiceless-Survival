package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class EZVCNetwork {
    public static SimpleNetworkWrapper INSTANCE;
    private static int packetId = 0;

    private static int nextID() {
        return packetId++;
    }

    public static void init() {
        EZVCSurvival.LOGGER.info("==============================================");
        EZVCSurvival.LOGGER.info("    Network Initialization Starting");
        EZVCSurvival.LOGGER.info("==============================================");

        try {
            // Crear el canal de red
            EZVCSurvival.LOGGER.info("Creating network channel: " + EZVCSurvival.MOD_ID);
            INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(EZVCSurvival.MOD_ID);

            if (INSTANCE == null) {
                throw new IllegalStateException("Failed to create network channel!");
            }

            EZVCSurvival.LOGGER.info("✓ Network channel created successfully");

            // Registrar paquetes
            EZVCSurvival.LOGGER.info("Registering packets...");
            registerPackets();

            EZVCSurvival.LOGGER.info("==============================================");
            EZVCSurvival.LOGGER.info("    Network Initialization Complete!");
            EZVCSurvival.LOGGER.info("    Total packets registered: " + packetId);
            EZVCSurvival.LOGGER.info("==============================================");

        } catch (Exception e) {
            EZVCSurvival.LOGGER.error("==============================================");
            EZVCSurvival.LOGGER.error("    NETWORK INITIALIZATION FAILED!");
            EZVCSurvival.LOGGER.error("==============================================");
            EZVCSurvival.LOGGER.error("Error type: " + e.getClass().getName());
            EZVCSurvival.LOGGER.error("Error message: " + e.getMessage());
            EZVCSurvival.LOGGER.error("Stack trace:", e);
            throw new RuntimeException("Failed to initialize network", e);
        }
    }

    private static void registerPackets() {
        try {
            // Packet 0: GeneralSoundPacket (SERVER)
            int id0 = nextID();
            EZVCSurvival.LOGGER.info("  [" + id0 + "] Registering GeneralSoundPacket (SERVER)...");
            INSTANCE.registerMessage(
                    GeneralSoundPacket.Handler.class,
                    GeneralSoundPacket.class,
                    id0,
                    Side.SERVER
            );
            EZVCSurvival.LOGGER.info("  ✓ GeneralSoundPacket registered");

        } catch (Exception e) {
            EZVCSurvival.LOGGER.error("  ✗ FAILED to register GeneralSoundPacket", e);
            throw new RuntimeException("Failed to register GeneralSoundPacket", e);
        }

        try {
            // Packet 1: OpenConfigEditorPacket (CLIENT)
            int id1 = nextID();
            EZVCSurvival.LOGGER.info("  [" + id1 + "] Registering OpenConfigEditorPacket (CLIENT)...");
            INSTANCE.registerMessage(
                    OpenConfigEditorPacket.Handler.class,
                    OpenConfigEditorPacket.class,
                    id1,
                    Side.CLIENT
            );
            EZVCSurvival.LOGGER.info("  ✓ OpenConfigEditorPacket registered");

        } catch (Exception e) {
            EZVCSurvival.LOGGER.error("  ✗ FAILED to register OpenConfigEditorPacket", e);
            throw new RuntimeException("Failed to register OpenConfigEditorPacket", e);
        }

        try {
            // Packet 2: UpdateConfigPacket (SERVER)
            int id2 = nextID();
            EZVCSurvival.LOGGER.info("  [" + id2 + "] Registering UpdateConfigPacket (SERVER)...");
            INSTANCE.registerMessage(
                    UpdateConfigPacket.Handler.class,
                    UpdateConfigPacket.class,
                    id2,
                    Side.SERVER
            );
            EZVCSurvival.LOGGER.info("  ✓ UpdateConfigPacket registered");

        } catch (Exception e) {
            EZVCSurvival.LOGGER.error("  ✗ FAILED to register UpdateConfigPacket", e);
            throw new RuntimeException("Failed to register UpdateConfigPacket", e);
        }
    }
}