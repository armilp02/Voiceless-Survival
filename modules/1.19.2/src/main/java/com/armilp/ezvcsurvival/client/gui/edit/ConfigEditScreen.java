package com.armilp.ezvcsurvival.client.gui.edit;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.GunfireConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.UpdateConfigPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigEditScreen extends Screen {

    public enum EditType {
        ENTITY_CONFIG,
        GENERAL_SOUND_CONFIG,
        GENERAL_SOUND_ENTITY,
        GUNFIRE_ENTITY
    }

    private final Screen parent;
    private final EditType editType;
    private final String elementId;
    private final String elementName;

    private Button enabledButton;
    private EditBox speedBox;
    private EditBox rangeBox;
    private EditBox thresholdBox;
    private Button priorityButton;
    private Button saveButton;
    private Button soundFiltersButton;

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

    private static final int TOP_MARGIN = 30;
    private static final int CENTER_X_OFFSET = 80;
    private static final int FIELD_WIDTH = 160;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_SPACING = 30;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;

    public ConfigEditScreen(Screen parent, EditType editType, String elementId, String elementName) {
        super(getTitleComponent(editType, elementName));
        this.parent = parent;
        this.editType = editType;
        this.elementId = elementId;
        this.elementName = elementName;

        loadCurrentValues();
    }

    private static Component getTitleComponent(EditType editType, String elementName) {
        switch (editType) {
            case ENTITY_CONFIG:
                return Component.translatable("screen.ezvcsurvival.entity_config_edit", elementName);
            case GENERAL_SOUND_CONFIG:
                return Component.translatable("screen.ezvcsurvival.general_sound_config_edit", elementName);
            case GENERAL_SOUND_ENTITY:
                return Component.translatable("screen.ezvcsurvival.general_sound_entity_edit", elementName);
            case GUNFIRE_ENTITY:
                return Component.translatable("screen.ezvcsurvival.gunfire_entity_edit", elementName);
            default:
                return Component.translatable("screen.ezvcsurvival.config_edit");
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
                java.util.Map<String, GeneralSoundsConfig.Reaction> reactions = GeneralSoundsConfig.getMobReactions();
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

            case GUNFIRE_ENTITY:
                GunfireConfig.Reaction gunfireReaction = GunfireConfig.getMobReactions().get(elementId);
                if (gunfireReaction != null) {
                    this.enabled = gunfireReaction.enabled;
                    this.speed = gunfireReaction.speed;
                    this.range = gunfireReaction.range;

                    this.originalEnabled = gunfireReaction.enabled;
                    this.originalSpeed = gunfireReaction.speed;
                    this.originalRange = gunfireReaction.range;
                } else {
                    this.enabled = true;
                    this.speed = 1.0;
                    this.range = 40.0;

                    this.originalEnabled = this.enabled;
                    this.originalSpeed = this.speed;
                    this.originalRange = this.range;
                }
                break;
        }
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = TOP_MARGIN + 40;

        this.clearWidgets();

        initFields(centerX, startY);
        initActionButtons(centerX, startY);
        updateSaveButtonState();
    }

    private void initFields(int centerX, int startY) {
        int currentY = startY;

        enabledButton = new Button(
                centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"),
                b -> toggleEnabled()
        );
        this.addRenderableWidget(enabledButton);
        currentY += FIELD_SPACING + 20;

        speedBox = new EditBox(this.font, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, Component.literal(""));
        speedBox.setValue(String.valueOf(speed));
        speedBox.setMaxLength(20);
        speedBox.setResponder(s -> {
            try {
                speed = Double.parseDouble(s);
                updateSaveButtonState();
            } catch (NumberFormatException ignored) {
            }
        });
        this.addRenderableWidget(speedBox);
        currentY += FIELD_SPACING + 20;

        rangeBox = new EditBox(this.font, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, Component.literal(""));
        rangeBox.setValue(String.valueOf(range));
        rangeBox.setMaxLength(20);
        rangeBox.setResponder(s -> {
            try {
                range = Double.parseDouble(s);
                updateSaveButtonState();
            } catch (NumberFormatException ignored) {
            }
        });
        this.addRenderableWidget(rangeBox);
        currentY += FIELD_SPACING + 20;

        if (editType == EditType.ENTITY_CONFIG) {
            thresholdBox = new EditBox(this.font, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, Component.literal(""));
            thresholdBox.setValue(String.valueOf(threshold));
            thresholdBox.setMaxLength(20);
            thresholdBox.setResponder(s -> {
                try {
                    threshold = Double.parseDouble(s);
                    updateSaveButtonState();
                } catch (NumberFormatException ignored) {
                }
            });
            this.addRenderableWidget(thresholdBox);
            currentY += FIELD_SPACING + 20;
        }

        if (editType == EditType.GENERAL_SOUND_CONFIG) {
            priorityButton = new Button(
                    centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT,
                    Component.translatable(isPriority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"),
                    b -> togglePriority()
            );
            this.addRenderableWidget(priorityButton);
            currentY += FIELD_SPACING + 20;
        }

        if (editType == EditType.GENERAL_SOUND_ENTITY) {
            soundFiltersButton = new Button(
                    centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT,
                    Component.literal("Sound Filters..."),
                    b -> Minecraft.getInstance().setScreen(new SoundFilterEditScreen(this, elementId, elementName))
            );
            this.addRenderableWidget(soundFiltersButton);
            currentY += FIELD_SPACING + 20;
        }
    }

    private void initActionButtons(int centerX, int startY) {
        int fieldCount = getFieldCount();
        int buttonY = startY + (fieldCount * FIELD_SPACING) + 30;

        this.saveButton = new Button(
                centerX - CENTER_X_OFFSET - BUTTON_WIDTH - BUTTON_SPACING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("button.ezvcsurvival.save"),
                b -> saveConfig()
        );
        this.addRenderableWidget(this.saveButton);

        Button cancelButton = new Button(
                centerX + CENTER_X_OFFSET, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("button.ezvcsurvival.cancel"),
                b -> Minecraft.getInstance().setScreen(parent)
        );
        this.addRenderableWidget(cancelButton);

        Button resetButton = new Button(
                centerX - BUTTON_WIDTH / 2, buttonY + BUTTON_SPACING + BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("button.ezvcsurvival.reset"),
                b -> resetToDefaults()
        );
        this.addRenderableWidget(resetButton);
    }

    private int getFieldCount() {
        switch (editType) {
            case ENTITY_CONFIG:
                return 4;
            case GENERAL_SOUND_CONFIG:
                return 4;
            case GENERAL_SOUND_ENTITY:
                return 4;
            case GUNFIRE_ENTITY:
                return 3;
            default:
                return 3;
        }
    }

    private void toggleEnabled() {
        enabled = !enabled;
        enabledButton.setMessage(Component.translatable(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"));
        updateSaveButtonState();
    }

    private void togglePriority() {
        isPriority = !isPriority;
        priorityButton.setMessage(Component.translatable(isPriority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"));
        updateSaveButtonState();
    }

    private void saveConfig() {
        try {
            try {
                speed = Double.parseDouble(speedBox.getValue());
                range = Double.parseDouble(rangeBox.getValue());
                if (thresholdBox != null) threshold = Double.parseDouble(thresholdBox.getValue());
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

                case GUNFIRE_ENTITY:
                    GunfireConfig.setMobReaction(elementId, enabled, speed, range);
                    GunfireConfig.persist();
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

                Minecraft.getInstance().setScreen(parent);
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
                if (thresholdBox != null) thresholdBox.setValue(String.valueOf(threshold));
                break;

            case GENERAL_SOUND_CONFIG:
                enabled = false;
                speed = 1.0;
                range = 1.0;
                isPriority = false;
                if (priorityButton != null) {
                    priorityButton.setMessage(Component.translatable(isPriority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"));
                }
                break;

            case GENERAL_SOUND_ENTITY:
                enabled = true;
                speed = 1.0;
                range = 50.0;
                break;

            case GUNFIRE_ENTITY:
                enabled = true;
                speed = 1.0;
                range = 40.0;
                break;
        }

        if (enabledButton != null) {
            enabledButton.setMessage(Component.translatable(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"));
        }
        if (speedBox != null) speedBox.setValue(String.valueOf(speed));
        if (rangeBox != null) rangeBox.setValue(String.valueOf(range));

        updateSaveButtonState();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        int titleY = TOP_MARGIN;
        drawCenteredString(poseStack, this.font, this.title, this.width / 2 + 1, titleY + 1, 0x88000000);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, titleY, 0xFFFFFF);

        String elementInfo = "ID: " + elementId;
        int elementInfoY = titleY + 18;
        drawCenteredString(poseStack, this.font, elementInfo, this.width / 2, elementInfoY, 0xAAAAAA);

        int lineY = elementInfoY + 10;
        int lineWidth = Math.min(180, this.width - 100);
        fill(poseStack, this.width / 2 - lineWidth / 2, lineY, this.width / 2 + lineWidth / 2, lineY + 1, 0x44FFFFFF);

        renderFieldLabels(poseStack);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void renderFieldLabels(PoseStack poseStack) {
        int centerX = this.width / 2;
        int startY = TOP_MARGIN + 40;
        int currentY = startY;

        this.font.draw(poseStack, "Enabled", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        currentY += FIELD_SPACING + 20;

        this.font.draw(poseStack, "Speed", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        currentY += FIELD_SPACING + 20;

        this.font.draw(poseStack, "Range", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        currentY += FIELD_SPACING + 20;

        if (editType == EditType.ENTITY_CONFIG) {
            this.font.draw(poseStack, "Threshold", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
            currentY += FIELD_SPACING + 20;
        }

        if (editType == EditType.GENERAL_SOUND_CONFIG) {
            this.font.draw(poseStack, "Priority Sound", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        }

        if (editType == EditType.GENERAL_SOUND_ENTITY) {
            this.font.draw(poseStack, "Configure Filters", centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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

                case GUNFIRE_ENTITY:
                    EZVCNetwork.INSTANCE.sendToServer(new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GUNFIRE_ENTITY, elementId, enabled, speed, range));
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
            case GUNFIRE_ENTITY:
                return enabled != originalEnabled ||
                        speed != originalSpeed ||
                        range != originalRange;

            default:
                return false;
        }
    }

    private void updateSaveButtonState() {
        if (saveButton != null) {
            boolean hasChanges = hasChanges();
            saveButton.active = hasChanges;

            if (hasChanges) {
                saveButton.setMessage(Component.translatable("button.ezvcsurvival.save"));
            } else {
                saveButton.setMessage(Component.translatable("button.ezvcsurvival.no_changes"));
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
            case GUNFIRE_ENTITY:
                originalEnabled = enabled;
                originalSpeed = speed;
                originalRange = range;
                break;
        }
    }

    private void showError() {
        System.err.println("[EZVCSurvival] Error: Values must be valid numbers.");
    }
}