package com.armilp.ezvcsurvival.client.gui.edit;

import com.armilp.ezvcsurvival.client.gui.list.ConfigListScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.GunfireConfig;
import com.armilp.ezvcsurvival.config.VoiceConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.UpdateConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigEditScreen extends Screen {
    
    public enum EditType {
        ENTITY_CONFIG,
        GENERAL_SOUND_CONFIG,
        GENERAL_SOUND_ENTITY,
        GUNFIRE_SOUND_CONFIG,
        GUNFIRE_ENTITY
    }
    
    private final Screen parent;
    private final EditType editType;
    private final String elementId;

    private Button enabledButton;
    private EditBox speedBox;
    private EditBox rangeBox;
    private EditBox thresholdBox;
    private Button priorityButton;
    private Button saveButton;

    private boolean enabled;
    private double speed;
    private double range;
    private double threshold;
    private boolean priority;

    private boolean originalEnabled;
    private double originalSpeed;
    private double originalRange;
    private double originalThreshold;
    private boolean originalPriority;

    private static final int TOP_MARGIN = 30;
    private static final int CENTER_X_OFFSET = 80;
    private static final int FIELD_WIDTH = 160;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_SPACING = 30;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;
    
    public ConfigEditScreen(Screen parent, EditType editType, String elementId, String elementName) {
        super(Component.translatable(getTitleKey(editType), elementName));
        this.parent = parent;
        this.editType = editType;
        this.elementId = elementId;
        
        loadCurrentValues();
    }
    
    private static String getTitleKey(EditType editType) {
        switch (editType) {
            case ENTITY_CONFIG: return "screen.ezvcsurvival.entity_config_edit";
            case GENERAL_SOUND_CONFIG: return "screen.ezvcsurvival.general_sound_config_edit";
            case GENERAL_SOUND_ENTITY: return "screen.ezvcsurvival.general_sound_entity_edit";
            case GUNFIRE_SOUND_CONFIG: return "screen.ezvcsurvival.gunfire_sound_config_edit";
            case GUNFIRE_ENTITY: return "screen.ezvcsurvival.gunfire_entity_edit";
            default: return "screen.ezvcsurvival.config_edit";
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
                }
                break;
                
            case GENERAL_SOUND_CONFIG:
                GeneralSoundsConfig.SoundEntry soundConfig = GeneralSoundsConfig.getSounds().get(elementId);
                if (soundConfig != null) {
                    this.enabled = soundConfig.enabled;
                    this.speed = soundConfig.speed_multiplier;
                    this.range = soundConfig.range_multiplier;

                    this.originalEnabled = soundConfig.enabled;
                    this.originalSpeed = soundConfig.speed_multiplier;
                    this.originalRange = soundConfig.range_multiplier;
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
                    // Valores por defecto por si la configuración aún no existe
                    this.enabled = true;
                    this.speed = 1.0;
                    this.range = 1.0;

                    this.originalEnabled = this.enabled;
                    this.originalSpeed = this.speed;
                    this.originalRange = this.range;
                }
                break;
                
            case GUNFIRE_SOUND_CONFIG:
                Boolean prioritySound = GunfireConfig.getGunPrioritySounds().get(elementId);
                this.priority = prioritySound != null ? prioritySound : false;

                this.originalPriority = this.priority;
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
                }
                break;
        }
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = TOP_MARGIN + 40;

        initFields(centerX, startY);
        initActionButtons(centerX, startY);

        updateSaveButtonState();
    }

    private void initFields(int centerX, int startY) {
        int currentY = startY;

        if (editType != EditType.GUNFIRE_SOUND_CONFIG) {
            enabledButton = Button.builder(
                    Component.translatable(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"), 
                    b -> toggleEnabled()
            ).bounds(centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT).build();
            this.addRenderableWidget(enabledButton);
            currentY += FIELD_SPACING + 20;
        }

        if (editType != EditType.GUNFIRE_SOUND_CONFIG) {
            speedBox = new EditBox(this.font, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, Component.literal(""));
            speedBox.setValue(String.valueOf(speed));
            speedBox.setMaxLength(20);
            speedBox.setResponder(s -> {
                try {
                    speed = Double.parseDouble(s);
                    updateSaveButtonState();
                } catch (NumberFormatException ignored) {}
            });
            this.addRenderableWidget(speedBox);
            currentY += FIELD_SPACING + 20;
        }
    
        // Campo de rango (para todos excepto GUNFIRE_SOUND_CONFIG)
        if (editType != EditType.GUNFIRE_SOUND_CONFIG) {
            rangeBox = new EditBox(this.font, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, Component.literal(""));
            rangeBox.setValue(String.valueOf(range));
            rangeBox.setMaxLength(20);
            rangeBox.setResponder(s -> {
                try {
                    range = Double.parseDouble(s);
                    updateSaveButtonState();
                } catch (NumberFormatException ignored) {}
            });
            this.addRenderableWidget(rangeBox);
            currentY += FIELD_SPACING + 20;
        }

        if (editType == EditType.ENTITY_CONFIG) {
            thresholdBox = new EditBox(this.font, centerX - CENTER_X_OFFSET, currentY, FIELD_WIDTH, FIELD_HEIGHT, Component.literal(""));
            thresholdBox.setValue(String.valueOf(threshold));
            thresholdBox.setMaxLength(20);
            thresholdBox.setResponder(s -> {
                try {
                    threshold = Double.parseDouble(s);
                    updateSaveButtonState();
                } catch (NumberFormatException ignored) {}
            });
            this.addRenderableWidget(thresholdBox);
        }

        if (editType == EditType.GUNFIRE_SOUND_CONFIG) {
            priorityButton = Button.builder(
                    Component.translatable(priority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"),
                    b -> togglePriority()
            ).bounds(centerX - CENTER_X_OFFSET, startY, FIELD_WIDTH, FIELD_HEIGHT).build();
            this.addRenderableWidget(priorityButton);
        }
    }

    private void initActionButtons(int centerX, int startY) {
        int fieldCount = getFieldCount();
        int buttonY = startY + (fieldCount * FIELD_SPACING) + 20;

        this.saveButton = Button.builder(
                Component.translatable("button.ezvcsurvival.save"), 
                b -> saveConfig()
        ).bounds(centerX - CENTER_X_OFFSET - BUTTON_WIDTH - BUTTON_SPACING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.saveButton);

        Button cancelButton = Button.builder(
                Component.translatable("button.ezvcsurvival.cancel"), 
                b -> Minecraft.getInstance().setScreen(parent)
        ).bounds(centerX + CENTER_X_OFFSET, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(cancelButton);

        Button resetButton = Button.builder(
                Component.translatable("button.ezvcsurvival.reset"), 
                b -> resetToDefaults()
        ).bounds(centerX - BUTTON_WIDTH / 2, buttonY + BUTTON_SPACING + BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(resetButton);
    }
    
    private int getFieldCount() {
        switch (editType) {
            case ENTITY_CONFIG: return 4;
            case GENERAL_SOUND_CONFIG: return 3;
            case GENERAL_SOUND_ENTITY: return 3;
            case GUNFIRE_SOUND_CONFIG: return 1;
            case GUNFIRE_ENTITY: return 3;
            default: return 3;
        }
    }

    private void toggleEnabled() {
        enabled = !enabled;
        enabledButton.setMessage(Component.translatable(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"));

        updateSaveButtonState();
    }
    
    private void togglePriority() {
        priority = !priority;
        priorityButton.setMessage(Component.translatable(priority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"));

        updateSaveButtonState();
    }

    private void saveConfig() {
        try {
            if (editType != EditType.GUNFIRE_SOUND_CONFIG) {
                if (speedBox != null) speed = Double.parseDouble(speedBox.getValue());
                if (rangeBox != null) range = Double.parseDouble(rangeBox.getValue());
                if (thresholdBox != null) threshold = Double.parseDouble(thresholdBox.getValue());
            }

            if (editType == EditType.ENTITY_CONFIG && thresholdBox != null) {
                if (threshold < -100.0) threshold = -100.0;
                if (threshold > 100.0) threshold = 100.0;
            }

            // Siempre aplicar localmente primero (para singleplayer y cliente)
            switch (editType) {
                case ENTITY_CONFIG:
                    EntityVoiceConfig.set(elementId, new EntityVoiceConfig.EntityConfig(enabled, speed, range, threshold));
                    EntityVoiceConfig.persist();
                    break;

                case GENERAL_SOUND_CONFIG:
                    GeneralSoundsConfig.setSoundEntry(elementId, enabled, speed, range);
                    GeneralSoundsConfig.persist();
                    break;

                case GENERAL_SOUND_ENTITY:
                    GeneralSoundsConfig.setMobReaction(elementId, enabled, speed, range);
                    GeneralSoundsConfig.persist();
                    break;

                case GUNFIRE_SOUND_CONFIG:
                    GunfireConfig.setPrioritySound(elementId, priority);
                    GunfireConfig.persist();
                    break;

                case GUNFIRE_ENTITY:
                    GunfireConfig.setMobReaction(elementId, enabled, speed, range);
                    GunfireConfig.persist();
                    break;
            }

            // Siempre enviar al servidor para sincronización
            sendUpdate();

            if (parent instanceof ConfigListScreen) {
                ((ConfigListScreen) parent).onConfigUpdated();
            }

            Minecraft.getInstance().setScreen(parent);
            
        } catch (NumberFormatException e) {
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
            case GENERAL_SOUND_ENTITY:
                enabled = true;
                speed = 1.0;
                range = 1.0;
                break;
                
            case GUNFIRE_SOUND_CONFIG:
                priority = false;
                break;
                
            case GUNFIRE_ENTITY:
                enabled = true;
                speed = 1.0;
                range = 50.0;
                break;
        }
        
        if (enabledButton != null) {
            enabledButton.setMessage(Component.translatable(enabled ? "button.ezvcsurvival.enabled" : "button.ezvcsurvival.disabled"));
        }
        if (speedBox != null) speedBox.setValue(String.valueOf(speed));
        if (rangeBox != null) rangeBox.setValue(String.valueOf(range));
        if (priorityButton != null) {
            priorityButton.setMessage(Component.translatable(priority ? "button.ezvcsurvival.priority_on" : "button.ezvcsurvival.priority_off"));
        }

        updateSaveButtonState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int titleY = TOP_MARGIN;
        graphics.drawCenteredString(this.font, this.title, this.width / 2 + 1, titleY + 1, 0x88000000);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xFFFFFF);

        String elementInfo = "ID: " + elementId;
        int elementInfoY = titleY + 18;
        graphics.drawCenteredString(this.font, elementInfo, this.width / 2, elementInfoY, 0xAAAAAA);

        int lineY = elementInfoY + 10;
        int lineWidth = Math.min(180, this.width - 100);
        graphics.fill(this.width / 2 - lineWidth / 2, lineY, this.width / 2 + lineWidth / 2, lineY + 1, 0x44FFFFFF);

        renderFieldLabels(graphics);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderFieldLabels(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int startY = TOP_MARGIN + 40;
        int currentY = startY;

        if (editType != EditType.GUNFIRE_SOUND_CONFIG) {
            if (enabledButton != null) {
                currentY += FIELD_SPACING + 20;
            }
            
            if (speedBox != null) {
                String label = editType == EditType.GENERAL_SOUND_CONFIG ? "label.ezvcsurvival.speed_multiplier" : "label.ezvcsurvival.speed";
                graphics.drawString(this.font, Component.translatable(label), centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
                currentY += FIELD_SPACING + 20;
            }
            
            if (rangeBox != null) {
                String label = editType == EditType.GENERAL_SOUND_CONFIG ? "label.ezvcsurvival.range_multiplier" : "label.ezvcsurvival.range";
                graphics.drawString(this.font, Component.translatable(label), centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
                currentY += FIELD_SPACING + 20;
            }
            
            if (thresholdBox != null) {
                graphics.drawString(this.font, Component.translatable("label.ezvcsurvival.threshold"), centerX - CENTER_X_OFFSET, currentY - 15, 0xFFFFFF);
            }
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
                    EZVCNetwork.INSTANCE.sendToServer(new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND, elementId, enabled, speed, range));
                    break;

                case GENERAL_SOUND_ENTITY:
                    EZVCNetwork.INSTANCE.sendToServer(new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND_ENTITY, elementId, enabled, speed, range));
                    break;

                case GUNFIRE_SOUND_CONFIG:
                    EZVCNetwork.INSTANCE.sendToServer(new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GUNFIRE_SOUND, elementId, priority, 0.0, 0.0));
                    break;
                    
                case GUNFIRE_ENTITY:
                    EZVCNetwork.INSTANCE.sendToServer(new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GUNFIRE_ENTITY, elementId, enabled, speed, range));
                    break;
            }

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Configuration sent to server: " + editType + " - " + elementId);
            }
        } catch (Exception e) {
            System.err.println("[EZVCSurvival] Error sending configuration: " + e.getMessage());
            e.printStackTrace();
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
            case GENERAL_SOUND_ENTITY:
                return enabled != originalEnabled || 
                       speed != originalSpeed || 
                       range != originalRange;
                       
            case GUNFIRE_SOUND_CONFIG:
                return priority != originalPriority;
                
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
    
    private void showError() {
        System.err.println("Error: " + "Values must be valid numbers.");
    }
}

