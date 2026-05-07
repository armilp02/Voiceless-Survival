package com.armilp.ezvcsurvival.network;

import com.armilp.ezvcsurvival.EZVCSurvival;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenConfigEditorPacket() implements CustomPacketPayload {

    public static final Type<OpenConfigEditorPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EZVCSurvival.MOD_ID, "open_config_editor"));

    public static final StreamCodec<ByteBuf, OpenConfigEditorPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenConfigEditorPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenConfigEditorPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                ClientPacketHandlers.handleOpenConfigEditor();
            }
        });
    }
}