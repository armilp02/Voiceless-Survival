package com.armilp.ezvcsurvival.client.gui.edit;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.UpdateConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.util.Map;

public class ConfigEditScreen extends GuiScreen {

    public enum EditType {
        ENTITY_CONFIG,
        GENERAL_SOUND_CONFIG,
        GENERAL_SOUND_ENTITY,
    }

    private final GuiScreen parent;
    private final EditType editType;
    private final String elementId;
    private final String elementName;

    private GuiButton enabledButton;
    private GuiTextField speedBox;
    private GuiTextField rangeBox;
    private GuiTextField thresholdBox;
    private GuiButton priorityButton;
    private GuiButton saveButton;
    private GuiButton soundFiltersButton;

    private boolean enabled;
    private double speed;
    private double range;
    private double threshold;
    private boolean isPriority;

    private boolean originalEnabled;
    private double originalSpeed;
    private double originalRange;
    private double originalThreshold;
    private boolean originalIsPriority;

    private String screenTitle;

    private static final int TOP_MARGIN = 30;
    private static final int CENTER_X_OFFSET = 80;
    private static final int FIELD_WIDTH = 160;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_SPACING = 30;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;

    public ConfigEditScreen(GuiScreen parent, EditType editType, String elementId, String elementName) {
        this.parent = parent;
        this.editType = editType;
        this.elementId = elementId;
        this.elementName = elementName;
        loadCurrentValues();
        this.screenTitle = getTitleString(editType, elementName);
    }

    private static String getTitleString(EditType editType, String elementName) {
        switch (editType) {
            case ENTITY_CONFIG:
                return I18n.format("screen.ezvcsurvival.entity_config_edit", elementName);
            case GENERAL_SOUND_CONFIG:
                return I18n.format("screen.ezvcsurvival.general_sound_config_edit", elementName);
            case GENERAL_SOUND_ENTITY:
                return I18n.format("screen.ezvcsurvival.general_sound_entity_edit", elementName);
            default:
                return I18n.format("screen.ezvcsurvival.config_edit");
        }
    }

    private void loadCurrentValues() {
        switch (editType) {
            case ENTITY_CONFIG:
                EntityVoiceConfig.EntityConfig entityConfig = EntityVoiceConfig.get(elementId);
                if (entityConfig != null) {
                    this.enabled = entityConfig.enabled;
                    this.speed = entityConfig.speed;
                    this.range = entityConfig.range;
                    this.threshold = entityConfig.threshold;
                    this.originalEnabled = entityConfig.enabled;
                    this.originalSpeed = entityConfig.speed;
                    this.originalRange = entityConfig.range;
                    this.originalThreshold = entityConfig.threshold;
                } else {
                    this.enabled = true;
                    this.speed = 1.0;
                    this.range = 50.0;
                    this.threshold = 0.0;
                    this.originalEnabled = this.enabled;
                    this.originalSpeed = this.speed;
                    this.originalRange = this.range;
                    this.originalThreshold = this.threshold;
                }
                break;
            case GENERAL_SOUND_CONFIG:
                GeneralSoundsConfig.SoundEntry soundConfig = GeneralSoundsConfig.getSounds().get(elementId);
                if (soundConfig != null) {
                    this.enabled = soundConfig.enabled;
                    this.speed = soundConfig.speed_multiplier;
                    this.range = soundConfig.range_multiplier;
                    this.isPriority = soundConfig.is_priority;
                    this.originalEnabled = soundConfig.enabled;
                    this.originalSpeed = soundConfig.speed_multiplier;
                    this.originalRange = soundConfig.range_multiplier;
                    this.originalIsPriority = soundConfig.is_priority;
                } else {
                    this.enabled = false;
                    this.speed = 1.0;
                    this.range = 1.0;
                    this.isPriority = false;
                    this.originalEnabled = this.enabled;
                    this.originalSpeed = this.speed;
                    this.originalRange = this.range;
                    this.originalIsPriority = this.isPriority;
                }
                break;
            case GENERAL_SOUND_ENTITY:
                Map<String, GeneralSoundsConfig.Reaction> reactions = GeneralSoundsConfig.getMobReactions();
                GeneralSoundsConfig.Reaction generalReaction = reactions != null ? reactions.get(elementId) : null;
                if (generalReaction != null) {
                    this.enabled = generalReaction.enabled;
                    this.speed = generalReaction.speed;
                    this.range = generalReaction.range;
                    this.originalEnabled = generalReaction.enabled;
                    this.originalSpeed = generalReaction.speed;
                    this.originalRange = generalReaction.range;
                } else {
                    this.enabled = true;
                    this.speed = 1.0;
                    this.range = 50.0;
                    this.originalEnabled = this.enabled;
                    this.originalSpeed = this.speed;
                    this.originalRange = this.range;
                }
                break;
        }
    }

    @Override
    public void initGui() {
        int centerX = this.width / 2;
        int startY = TOP_MARGIN + 40;
        this.buttonList.clear();
        initFields(centerX, startY);
        initActionButtons(centerX, startY);
        updateSaveButtonState();
    }

    private void initFields(int centerX, int startY) {
        int currentY = startY;
        enabledButton = new GuiButton(0, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, I18n.format(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"));
        this.buttonList.add(enabledButton);
        currentY += FIELD_SPACING + 20;
        speedBox = new GuiTextField(1, this.fontRenderer, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT);
        speedBox.setText(String.valueOf(speed));
        speedBox.setMaxStringLength(20);
        currentY += FIELD_SPACING + 20;
        rangeBox = new GuiTextField(2, this.fontRenderer, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT);
        rangeBox.setText(String.valueOf(range));
        rangeBox.setMaxStringLength(20);
        currentY += FIELD_SPACING + 20;
        if (editType == EditType.ENTITY_CONFIG) {
            thresholdBox = new GuiTextField(3, this.fontRenderer, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT);
            thresholdBox.setText(String.valueOf(threshold));
            thresholdBox.setMaxStringLength(20);
            currentY += FIELD_SPACING + 20;
        }
        if (editType == EditType.GENERAL_SOUND_CONFIG) {
            priorityButton = new GuiButton(4, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, I18n.format(isPriority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"));
            this.buttonList.add(priorityButton);
            currentY += FIELD_SPACING + 20;
        }
        if (editType == EditType.GENERAL_SOUND_ENTITY) {
            soundFiltersButton = new GuiButton(5, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, "Sound Filters...");
            this.buttonList.add(soundFiltersButton);
        }
    }

    private void initActionButtons(int centerX, int startY) {
        int fieldCount = getFieldCount();
        int buttonY = startY + (fieldCount * FIELD_SPACING) + 30;
        saveButton = new GuiButton(10, centerX - CENTER_X_OFFSET - BUTTON_WIDTH - BUTTON_SPACING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.format("button.ezvcsurvival.save"));
        this.buttonList.add(saveButton);
        GuiButton cancelButton = new GuiButton(11, centerX + CENTER_X_OFFSET, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.format("button.ezvcsurvival.cancel"));
        this.buttonList.add(cancelButton);
        GuiButton resetButton = new GuiButton(12, centerX - BUTTON_WIDTH / 2, buttonY + BUTTON_SPACING + BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, I18n.format("button.ezvcsurvival.reset"));
        this.buttonList.add(resetButton);
    }

    private int getFieldCount() {
        switch (editType) {
            case ENTITY_CONFIG:
                return 4;
            case GENERAL_SOUND_CONFIG:
                return 4;
            case GENERAL_SOUND_ENTITY:
                return 4;
            default:
                return 3;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null) return;
        if (!button.enabled) return;
        if (button == enabledButton) {
            toggleEnabled();
            return;
        }
        if (button == priorityButton) {
            togglePriority();
            return;
        }
        if (button == saveButton) {
            saveConfig();
            return;
        }
        if (button == soundFiltersButton) {
            Minecraft.getMinecraft().displayGuiScreen(new SoundFilterEditScreen(this, elementId, elementName));
            return;
        }
        if (button.id == 11) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }
        if (button.id == 12) {
            resetToDefaults();
            return;
        }
    }

    private void toggleEnabled() {
        enabled = !enabled;
        enabledButton.displayString = I18n.format(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled");
        updateSaveButtonState();
    }

    private void togglePriority() {
        isPriority = !isPriority;
        priorityButton.displayString = I18n.format(isPriority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off");
        updateSaveButtonState();
    }

    private void saveConfig() {
        try {
            try {
                speed = Double.parseDouble(speedBox.getText());
                range = Double.parseDouble(rangeBox.getText());
                if (thresholdBox != null) threshold = Double.parseDouble(thresholdBox.getText());
            } catch (NumberFormatException e) {
                showError();
                return;
            }
            if (editType == EditType.ENTITY_CONFIG && thresholdBox != null) {
                if (threshold < -100.0) threshold = -100.0;
                if (threshold > 100.0) threshold = 100.0;
            }
            boolean localSuccess = false;
            switch (editType) {
                case ENTITY_CONFIG:
                    EntityVoiceConfig.set(elementId, new EntityVoiceConfig.EntityConfig(enabled, speed, range, threshold));
                    EntityVoiceConfig.persist();
                    localSuccess = true;
                    break;
                case GENERAL_SOUND_CONFIG:
                    GeneralSoundsConfig.setSoundEntry(elementId, enabled, speed, range, isPriority);
                    GeneralSoundsConfig.persist();
                    GeneralSoundsConfig.SoundEntry updated = GeneralSoundsConfig.getSounds().get(elementId);
                    localSuccess = (updated != null &&
                            updated.enabled == enabled &&
                            Math.abs(updated.speed_multiplier - speed) < 0.001 &&
                            Math.abs(updated.range_multiplier - range) < 0.001 &&
                            updated.is_priority == isPriority);
                    break;
                case GENERAL_SOUND_ENTITY:
                    GeneralSoundsConfig.setMobReaction(elementId, enabled, speed, range);
                    GeneralSoundsConfig.persist();
                    localSuccess = true;
                    break;
            }
            if (localSuccess) {
                try {
                    com.armilp.ezvcsurvival.config.SoundConfig.loadConfigs();
                } catch (Exception e) {
                }
                sendUpdate();
                updateOriginalValues();
                if (parent instanceof ConfigListScreen) {
                    ((ConfigListScreen) parent).onConfigUpdated();
                }
                Minecraft.getMinecraft().displayGuiScreen(parent);
            } else {
                showError();
            }
        } catch (Exception e) {
            showError();
        }
    }

    private void resetToDefaults() {
        switch (editType) {
            case ENTITY_CONFIG:
                EntityVoiceConfig.EntityConfig defaultConfig = EntityVoiceConfig.EntityConfig.defaultFor(null);
                enabled = defaultConfig.enabled;
                speed = defaultConfig.speed;
                range = defaultConfig.range;
                threshold = defaultConfig.threshold;
                if (thresholdBox != null) thresholdBox.setText(String.valueOf(threshold));
                break;
            case GENERAL_SOUND_CONFIG:
                enabled = false;
                speed = 1.0;
                range = 1.0;
                isPriority = false;
                if (priorityButton != null) {
                    priorityButton.displayString = I18n.format(isPriority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off");
                }
                break;
            case GENERAL_SOUND_ENTITY:
                enabled = true;
                speed = 1.0;
                range = 50.0;
                break;
        }
        if (enabledButton != null) {
            enabledButton.displayString = I18n.format(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled");
        }
        if (speedBox != null) speedBox.setText(String.valueOf(speed));
        if (rangeBox != null) rangeBox.setText(String.valueOf(range));
        updateSaveButtonState();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int titleY = TOP_MARGIN;
        drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2 + 1, titleY + 1, 0x88000000);
        drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, titleY, 0xFFFFFF);
        String elementInfo = "ID: " + elementId;
        int elementInfoY = titleY + 18;
        drawCenteredString(this.fontRenderer, elementInfo, this.width / 2, elementInfoY, 0xAAAAAA);
        int lineY = elementInfoY + 10;
        int lineWidth = Math.min(180, this.width - 100);
        drawRect(this.width / 2 - lineWidth / 2, lineY, this.width / 2 + lineWidth / 2, lineY + 1, 0x44FFFFFF);
        renderFieldLabels();
        if (speedBox != null) speedBox.drawTextBox();
        if (rangeBox != null) rangeBox.drawTextBox();
        if (thresholdBox != null) thresholdBox.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderFieldLabels() {
        int centerX = this.width / 2;
        int startY = TOP_MARGIN + 40;
        int currentY = startY;
        this.fontRenderer.drawString("Enabled", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        currentY += FIELD_SPACING + 20;
        this.fontRenderer.drawString("Speed", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        currentY += FIELD_SPACING + 20;
        this.fontRenderer.drawString("Range", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        currentY += FIELD_SPACING + 20;
        if (editType == EditType.ENTITY_CONFIG) {
            this.fontRenderer.drawString("Threshold", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
            currentY += FIELD_SPACING + 20;
        }
        if (editType == EditType.GENERAL_SOUND_CONFIG) {
            this.fontRenderer.drawString("Priority Sound", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        }
        if (editType == EditType.GENERAL_SOUND_ENTITY) {
            this.fontRenderer.drawString("Configure Filters", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        boolean consumed = false;
        if (speedBox != null && speedBox.textboxKeyTyped(typedChar, keyCode)) {
            try {
                speed = Double.parseDouble(speedBox.getText());
                updateSaveButtonState();
            } catch (NumberFormatException ignored) {
            }
            consumed = true;
        }
        if (rangeBox != null && rangeBox.textboxKeyTyped(typedChar, keyCode)) {
            try {
                range = Double.parseDouble(rangeBox.getText());
                updateSaveButtonState();
            } catch (NumberFormatException ignored) {
            }
            consumed = true;
        }
        if (thresholdBox != null && thresholdBox.textboxKeyTyped(typedChar, keyCode)) {
            try {
                threshold = Double.parseDouble(thresholdBox.getText());
                updateSaveButtonState();
            } catch (NumberFormatException ignored) {
            }
            consumed = true;
        }
        if (consumed) return;
        if (keyCode == 1) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (speedBox != null) speedBox.mouseClicked(mouseX, mouseY, mouseButton);
        if (rangeBox != null) rangeBox.mouseClicked(mouseX, mouseY, mouseButton);
        if (thresholdBox != null) thresholdBox.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void sendUpdate() {
        try {
            switch (editType) {
                case ENTITY_CONFIG:
                    EZVCNetwork.INSTANCE.sendToServer(new UpdateConfigPacket(elementId, enabled, speed, range, threshold));
                    break;
                case GENERAL_SOUND_CONFIG:
                    EZVCNetwork.INSTANCE.sendToServer(new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND, elementId, enabled, speed, range, isPriority));
                    break;
                case GENERAL_SOUND_ENTITY:
                    EZVCNetwork.INSTANCE.sendToServer(new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND_ENTITY, elementId, enabled, speed, range));
                    break;
            }
        } catch (Exception e) {
            System.err.println("[EZVCSurvival] Error sending configuration: " + e.getMessage());
        }
    }

    private boolean hasChanges() {
        switch (editType) {
            case ENTITY_CONFIG:
                return enabled != originalEnabled ||
                        speed != originalSpeed ||
                        range != originalRange ||
                        threshold != originalThreshold;
            case GENERAL_SOUND_CONFIG:
                return enabled != originalEnabled ||
                        speed != originalSpeed ||
                        range != originalRange ||
                        isPriority != originalIsPriority;
            case GENERAL_SOUND_ENTITY:
            default:
                return false;
        }
    }

    private void updateSaveButtonState() {
        if (saveButton != null) {
            boolean hasChanges = hasChanges();
            saveButton.enabled = hasChanges;
            if (hasChanges) {
                saveButton.displayString = I18n.format("button.ezvcsurvival.save");
            } else {
                saveButton.displayString = I18n.format("button.ezvcsurvival.no_changes");
            }
        }
    }

    private void updateOriginalValues() {
        switch (editType) {
            case ENTITY_CONFIG:
                originalEnabled = enabled;
                originalSpeed = speed;
                originalRange = range;
                originalThreshold = threshold;
                break;
            case GENERAL_SOUND_CONFIG:
                originalEnabled = enabled;
                originalSpeed = speed;
                originalRange = range;
                originalIsPriority = isPriority;
                break;
            case GENERAL_SOUND_ENTITY:
        }
    }

    private void showError() {
        System.err.println("[EZVCSurvival] Error: Values must be valid numbers.");
    }
}