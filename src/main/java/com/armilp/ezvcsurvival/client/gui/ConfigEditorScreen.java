package com.armilp.ezvcsurvival.client.gui;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.util.ModInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

public class ConfigEditorScreen extends Screen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 20;

    private int centerX;
    private int centerY;
    private int startY;

    public ConfigEditorScreen() {
        super(new TranslatableComponent("screen.ezvcsurvival.config_editor"));
    }

    @Override
    protected void init() {
        super.init();

        centerX = this.width / 2;
        centerY = this.height / 2;

        startY = Math.max(centerY - 30, 70);

        this.addRenderableWidget(new Button(
                centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslatableComponent("button.ezvcsurvival.entity_config"),
                btn -> Minecraft.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.ENTITY_CONFIG, this))
        ));

        this.addRenderableWidget(new Button(
                centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslatableComponent("button.ezvcsurvival.general_sounds"),
                btn -> Minecraft.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.GENERAL_SOUNDS_CONFIG, this))
        ));

        this.addRenderableWidget(new Button(
                centerX - BUTTON_WIDTH / 2, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslatableComponent("button.ezvcsurvival.gunfire_config"),
                btn -> Minecraft.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.GUNFIRE_CONFIG, this))
        ));
    }

    @Override
    public void renderBackground(@NotNull PoseStack poseStack) {
        super.renderBackground(poseStack);
        // Fill background with semi-transparent black
        fill(poseStack, 0, 0, this.width, this.height, 0x40000000);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        int titleY = Math.max(30, this.height / 10);
        // Draw title with shadow effect
        drawCenteredString(poseStack, this.font, this.title, centerX + 1, titleY + 1, 0x88000000);
        drawCenteredString(poseStack, this.font, this.title, centerX, titleY, 0xFFFFFF);

        Component subtitle = new TranslatableComponent("screen.ezvcsurvival.config_editor.subtitle");
        int subtitleY = titleY + 20;
        drawCenteredString(poseStack, this.font, subtitle, centerX, subtitleY, 0xCCCCCC);

        int subtitleWidth = this.font.width(subtitle);
        int lineY = subtitleY + 12;
        int lineWidth = Math.min(subtitleWidth + 30, this.width - 60);
        fill(poseStack, centerX - lineWidth / 2, lineY,
                centerX + lineWidth / 2, lineY + 1, 0x44FFFFFF);

        Component info = new TranslatableComponent("screen.ezvcsurvival.config_editor.info");
        int infoY = lineY + 15;
        drawCenteredString(poseStack, this.font, info, centerX, infoY, 0x888888);

        Component version = ModInfo.getVersion();
        int versionWidth = this.font.width(version);
        this.font.draw(poseStack, version, this.width - versionWidth - 10, 10, 0x666666);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        centerX = this.width / 2;
        centerY = this.height / 2;
        startY = Math.max(centerY - 30, 70);
    }
}