package com.armilp.ezvcsurvival.voicechat.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class DbMeterOverlay implements IGuiOverlay {

    public static final DbMeterOverlay INSTANCE = new DbMeterOverlay();

    private static final ResourceLocation BAR_EMPTY = ResourceLocation.fromNamespaceAndPath("ezvcsurvival", "textures/gui/exp_bar.png");
    private static final ResourceLocation BAR_FULL = ResourceLocation.fromNamespaceAndPath("ezvcsurvival", "textures/gui/exp_bar_full.png");
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 6;
    private static final int X = 30;
    private static final int Y_OFFSET_FROM_BOTTOM = 24;
    private static final float TEXT_SCALE = 0.7f;
    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        double level = ClientVoiceLevel.getLevel();
        if (level <= 0.01) return;

        int y = screenHeight - Y_OFFSET_FROM_BOTTOM;
        int filledWidth = (int) Math.ceil(BAR_WIDTH * level);

        guiGraphics.blit(BAR_EMPTY, X, y, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        if (filledWidth > 0) {
            guiGraphics.blit(BAR_FULL, X, y, 0, 0, filledWidth, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        double db = ClientVoiceLevel.MIN_DB + level * (ClientVoiceLevel.MAX_DB - ClientVoiceLevel.MIN_DB);
        Font font = gui.getFont();
        String text = Math.round(db) + " dB";

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(X + BAR_WIDTH + 5, y - 1, 0);
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
        guiGraphics.drawString(font, text, 0, 0, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }
}