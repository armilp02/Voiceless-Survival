package com.armilp.ezvcsurvival.client.gui;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.util.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ConfigEditorScreen extends Screen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 24;

    private int centerX;
    private int centerY;
    private int startY;

    public ConfigEditorScreen() {
        super(Component.translatable("screen.ezvcsurvival.config_editor"));
    }

    @Override
    protected void init() {
        super.init();

        centerX = this.width / 2;
        centerY = this.height / 2;

        startY = Math.max(centerY - 10, 90);

        this.addRenderableWidget(Button.builder(
                Component.translatable("button.ezvcsurvival.entity_config"),
                btn -> Minecraft.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.ENTITY_CONFIG, this))
        ).bounds(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("button.ezvcsurvival.general_sounds"),
                btn -> Minecraft.getInstance().setScreen(new ConfigListScreen(ConfigListScreen.ListType.GENERAL_SOUNDS_CONFIG, this))
        ).bounds(centerX - BUTTON_WIDTH / 2, startY + BUTTON_HEIGHT + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Renderizar los widgets (botones) primero
        super.render(graphics, mouseX, mouseY, partialTick);

        // Renderizar textos directamente sin manipular pose
        int titleY = Math.max(30, this.height / 10);

        // Título - PRIMERO el texto, LUEGO el fondo para que el fondo no tape el texto
        graphics.drawCenteredString(this.font, this.title, centerX, titleY, 0xFFFFFFFF);

        // Subtítulo
        Component subtitle = Component.translatable("screen.ezvcsurvival.config_editor.subtitle");
        int subtitleY = titleY + 15;
        graphics.drawCenteredString(this.font, subtitle, centerX, subtitleY, 0xFFCCCCCC);

        // Línea decorativa
        int subtitleWidth = this.font.width(subtitle);
        int lineY = subtitleY + 10;
        int lineWidth = Math.min(subtitleWidth + 40, this.width - 80);
        graphics.fill(centerX - lineWidth / 2, lineY,
                centerX + lineWidth / 2, lineY + 1, 0x88FFFFFF);

        // Información adicional
        Component info = Component.translatable("screen.ezvcsurvival.config_editor.info");
        int infoY = lineY + 12;
        graphics.drawCenteredString(this.font, info, centerX, infoY, 0xFFAAAAAA);

        // Versión en la esquina
        Component version = ModInfo.getVersion();
        int versionWidth = this.font.width(version);
        graphics.drawString(this.font, version, this.width - versionWidth - 5, 5, 0xFFAAAAAA, true);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getDigit() == 256) { // ESC
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}