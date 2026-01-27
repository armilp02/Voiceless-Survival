package com.armilp.ezvcsurvival.client.gui;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.io.IOException;

public class ConfigEditorScreen extends GuiScreen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 20;

    private int centerX;
    private int centerY;
    private int startY;

    @Override
    public void initGui() {
        super.initGui();

        centerX = this.width / 2;
        centerY = this.height / 2;
        startY = Math.max(centerY - 30, 70);

        this.buttonList.clear();

        this.buttonList.add(new GuiButton(
                0,
                centerX - BUTTON_WIDTH / 2,
                startY,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                I18n.format("button.ezvcsurvival.entity_config")
        ));

        this.buttonList.add(new GuiButton(
                1,
                centerX - BUTTON_WIDTH / 2,
                startY + BUTTON_SPACING,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                I18n.format("button.ezvcsurvival.general_sounds")
        ));

        this.buttonList.add(new GuiButton(
                2,
                centerX - BUTTON_WIDTH / 2,
                startY + BUTTON_SPACING * 2,
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                I18n.format("button.ezvcsurvival.gunfire_config")
        ));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                this.mc.displayGuiScreen(new ConfigListScreen(ConfigListScreen.ListType.ENTITY_CONFIG, this));
                break;
            case 1:
                this.mc.displayGuiScreen(new ConfigListScreen(ConfigListScreen.ListType.GENERAL_SOUNDS_CONFIG, this));
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // Fill background with semi-transparent black
        drawRect(0, 0, this.width, this.height, 0x40000000);

        int titleY = Math.max(30, this.height / 10);
        String title = I18n.format("screen.ezvcsurvival.config_editor");

        // Draw title with shadow effect
        this.drawCenteredString(this.fontRenderer, title, centerX + 1, titleY + 1, 0x88000000);
        this.drawCenteredString(this.fontRenderer, title, centerX, titleY, 0xFFFFFF);

        String subtitle = I18n.format("screen.ezvcsurvival.config_editor.subtitle");
        int subtitleY = titleY + 20;
        this.drawCenteredString(this.fontRenderer, subtitle, centerX, subtitleY, 0xCCCCCC);

        int subtitleWidth = this.fontRenderer.getStringWidth(subtitle);
        int lineY = subtitleY + 12;
        int lineWidth = Math.min(subtitleWidth + 30, this.width - 60);
        drawRect(centerX - lineWidth / 2, lineY, centerX + lineWidth / 2, lineY + 1, 0x44FFFFFF);

        String info = I18n.format("screen.ezvcsurvival.config_editor.info");
        int infoY = lineY + 15;
        this.drawCenteredString(this.fontRenderer, info, centerX, infoY, 0x888888);

//        String version = String.valueOf(ModInfo.getVersion());
//        int versionWidth = this.fontRenderer.getStringWidth(version);
//        this.fontRenderer.drawString(version, this.width - versionWidth - 10, 10, 0x666666);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC key
            this.mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        centerX = this.width / 2;
        centerY = this.height / 2;
        startY = Math.max(centerY - 30, 70);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}