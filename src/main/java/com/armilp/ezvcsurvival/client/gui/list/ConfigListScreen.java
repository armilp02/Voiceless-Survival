package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class ConfigListScreen extends Screen {

    public enum ListType {
        ENTITY_CONFIG,
        GENERAL_SOUNDS_CONFIG,
        GUNFIRE_CONFIG
    }

    private final ListType listType;
    private final Screen parent;

    private EditBox searchBox;
    private String searchQuery = "";
    private Button clearSearchButton;
    private Button refreshButton;
    private Button backButton;
    private Button toggleViewButton;
    private Button toggleEnabledButton;
    private ConfigListWidget list;

    private List<Object> items;
    private boolean showingSounds = true;

    // Constantes mejoradas para responsive design
    private static final int MIN_WIDTH = 400;
    private static final int MIN_BUTTON_WIDTH = 60;
    private static final int MAX_BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SEARCH_HEIGHT = 20;
    private static final int VERTICAL_SPACING = 6;
    private static final int HORIZONTAL_SPACING = 6;

    public ConfigListScreen(ListType listType, Screen parent) {
        super(Component.translatable(getTitleKey(listType)));
        this.listType = listType;
        this.parent = parent;
        this.items = new ArrayList<>();

        if (this.listType == ListType.GUNFIRE_CONFIG) {
            this.showingSounds = false;
        } else if (this.listType == ListType.GENERAL_SOUNDS_CONFIG) {
            this.showingSounds = false;
        }
    }

    private static String getTitleKey(ListType type) {
        switch (type) {
            case ENTITY_CONFIG: return "screen.ezvcsurvival.entity_config_list";
            case GENERAL_SOUNDS_CONFIG: return "screen.ezvcsurvival.general_sounds_config_list";
            case GUNFIRE_CONFIG: return "screen.ezvcsurvival.gunfire_config_list";
            default: return "screen.ezvcsurvival.config_list";
        }
    }

    @Override
    protected void init() {
        super.init();

        int usableWidth = Math.max(this.width - 40, MIN_WIDTH);
        int leftMargin = (this.width - usableWidth) / 2;
        int rightMargin = leftMargin;

        int topRowY = 20;
        int searchRowY = topRowY + BUTTON_HEIGHT + VERTICAL_SPACING * 2;
        int listStartY = searchRowY + SEARCH_HEIGHT + VERTICAL_SPACING * 3;

        int availableButtonWidth = usableWidth;
        int buttonCount = getButtonCount();
        int buttonWidth = Math.min(MAX_BUTTON_WIDTH, Math.max(MIN_BUTTON_WIDTH,
                (availableButtonWidth - (buttonCount - 1) * HORIZONTAL_SPACING) / buttonCount));

        backButton = new Button(
                leftMargin, topRowY, buttonWidth, BUTTON_HEIGHT,
                Component.translatable("button.ezvcsurvival.back"),
                b -> Minecraft.getInstance().setScreen(parent != null ? parent : new ConfigEditorScreen())
        );
        this.addRenderableWidget(backButton);

        int currentX = leftMargin + buttonWidth + HORIZONTAL_SPACING;

        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            toggleViewButton = new Button(
                    currentX, topRowY, buttonWidth, BUTTON_HEIGHT,
                    Component.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"),
                    b -> toggleView()
            );
            this.addRenderableWidget(toggleViewButton);
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        if (listType == ListType.GENERAL_SOUNDS_CONFIG || listType == ListType.GUNFIRE_CONFIG || listType == ListType.ENTITY_CONFIG) {
            toggleEnabledButton = new Button(
                    currentX, topRowY, buttonWidth, BUTTON_HEIGHT,
                    getToggleEnabledMessage(),
                    b -> toggleEnabled()
            );
            this.addRenderableWidget(toggleEnabledButton);
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        refreshButton = new Button(
                this.width - rightMargin - buttonWidth, topRowY, buttonWidth, BUTTON_HEIGHT,
                Component.translatable("button.ezvcsurvival.refresh"),
                b -> refreshData()
        );
        this.addRenderableWidget(refreshButton);

        int searchWidth = Math.min(300, usableWidth - 100);
        searchBox = new EditBox(this.font, leftMargin, searchRowY, searchWidth, SEARCH_HEIGHT,
                Component.translatable("textbox.ezvcsurvival.search"));
        searchBox.setResponder(this::onSearchChanged);
        searchBox.setMaxLength(50);
        this.addRenderableWidget(searchBox);

        clearSearchButton = new Button(
                leftMargin + searchWidth + HORIZONTAL_SPACING, searchRowY, 20, SEARCH_HEIGHT,
                Component.literal("✕"),
                b -> searchBox.setValue("")
        );
        this.addRenderableWidget(clearSearchButton);

        // Lista sin scroll bar visible
        this.list = new ConfigListWidget(this, this.minecraft, this.width, this.height,
                listStartY, this.height - 40, 28);
        this.addWidget(this.list);

        if (items == null || items.isEmpty()) {
            loadData();
        }

        updateList();
    }

    private int getButtonCount() {
        int count = 2; // Back + Refresh
        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            count += 2; // Toggle View + Toggle Enabled
        } else if (listType == ListType.GUNFIRE_CONFIG || listType == ListType.ENTITY_CONFIG) {
            count += 1; // Toggle Enabled
        }
        return count;
    }

    private void loadData() {
        items = new ArrayList<>();

        switch (listType) {
            case ENTITY_CONFIG:
                loadEntityConfigs();
                break;
            case GENERAL_SOUNDS_CONFIG:
                if (showingSounds) {
                    loadGeneralSoundConfigs();
                } else {
                    loadGeneralEntityConfigs();
                }
                break;
            case GUNFIRE_CONFIG:
                loadGunfireEntityConfigs();
                break;
        }
    }

    private void loadEntityConfigs() {
        items.clear();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;

            String id = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(type)).toString();
            EntityVoiceConfig.EntityConfig config = EntityVoiceConfig.get(id);

            if (config == null) {
                config = EntityVoiceConfig.EntityConfig.defaultFor(type);
                EntityVoiceConfig.set(id, config);
            }

            items.add(new EntityConfigItem(id, type, config));
        }
        items.sort(Comparator.comparing(item -> ((EntityConfigItem) item).getDisplayName()));
    }

    private void loadGeneralSoundConfigs() {
        items.clear();

        Map<String, GeneralSoundsConfig.SoundEntry> soundConfigs = GeneralSoundsConfig.getSounds();

        for (SoundEvent sound : ForgeRegistries.SOUND_EVENTS) {
            String id = Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getKey(sound)).toString();

            GeneralSoundsConfig.SoundEntry config = soundConfigs.get(id);
            if (config == null) {
                config = new GeneralSoundsConfig.SoundEntry(false, 1.0, 1.0);
                GeneralSoundsConfig.setSoundEntry(id, false, 1.0, 1.0);
            }

            items.add(new SoundConfigItem(id, sound, config, false));
        }

        items.sort(Comparator.comparing(item -> ((SoundConfigItem) item).getId()));
    }

    private void loadGeneralEntityConfigs() {
        items.clear();
        Map<String, GeneralSoundsConfig.Reaction> entityConfigs = GeneralSoundsConfig.getMobReactions();
        if (entityConfigs == null) {
            entityConfigs = java.util.Collections.emptyMap();
        }

        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;

            String id = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(type)).toString();
            GeneralSoundsConfig.Reaction reaction = entityConfigs.get(id);

            if (reaction == null) {
                reaction = new GeneralSoundsConfig.Reaction(true, 1.0, category == MobCategory.MONSTER ? 60.0 : 50.0);
                GeneralSoundsConfig.setMobReaction(id, true, 1.0, category == MobCategory.MONSTER ? 60.0 : 50.0);
            }

            items.add(new EntityReactionItem(id, reaction));
        }

        items.sort(Comparator.comparing(item -> ((EntityReactionItem) item).getId()));
    }

    private void loadGunfireEntityConfigs() {
        items.clear();
        Map<String, GunfireConfig.Reaction> entityConfigs = GunfireConfig.getMobReactions();

        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;

            String id = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(type)).toString();
            GunfireConfig.Reaction reaction = entityConfigs.get(id);

            if (reaction == null) {
                reaction = new GunfireConfig.Reaction(true, 1.0, category == MobCategory.MONSTER ? 50.0 : 40.0);
                GunfireConfig.setMobReaction(id, true, 1.0, category == MobCategory.MONSTER ? 50.0 : 40.0);
            }

            items.add(new GunfireEntityItem(id, reaction));
        }

        items.sort(Comparator.comparing(item -> ((GunfireEntityItem) item).getId()));
    }

    private void toggleView() {
        if (listType != ListType.GENERAL_SOUNDS_CONFIG) return;

        showingSounds = !showingSounds;
        toggleViewButton.setMessage(Component.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"));

        safeRefresh();
    }

    private void toggleEnabled() {
        switch (listType) {
            case ENTITY_CONFIG:
                boolean newEntityState = !EntityVoiceConfig.isEnabled();
                EntityVoiceConfig.ROOT.enabled = newEntityState;
                EntityVoiceConfig.persist();

                EZVCNetwork.INSTANCE.sendToServer(
                        new UpdateConfigPacket(UpdateConfigPacket.ConfigType.ENTITY_VOICE,
                                "global", newEntityState, 1.0, 1.0)
                );
                break;
            case GENERAL_SOUNDS_CONFIG:
                boolean newGeneralState = !GeneralSoundsConfig.isEnabled();
                GeneralSoundsConfig.ROOT.enabled = newGeneralState;
                GeneralSoundsConfig.persist();

                EZVCNetwork.INSTANCE.sendToServer(
                        new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND,
                                "global", newGeneralState, 1.0, 1.0)
                );
                break;

            case GUNFIRE_CONFIG:
                boolean newGunfireState = !GunfireConfig.isEnabled();
                GunfireConfig.ROOT.enabled = newGunfireState;
                GunfireConfig.persist();

                EZVCNetwork.INSTANCE.sendToServer(
                        new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GUNFIRE_SOUND,
                                "global", newGunfireState, 1.0, 1.0)
                );
                break;
        }
        toggleEnabledButton.setMessage(getToggleEnabledMessage());
        safeRefresh();
    }

    private Component getToggleEnabledMessage() {
        boolean isEnabled = false;
        switch (listType) {
            case ENTITY_CONFIG:
                isEnabled = EntityVoiceConfig.isEnabled();
                break;
            case GENERAL_SOUNDS_CONFIG:
                isEnabled = GeneralSoundsConfig.isEnabled();
                break;
            case GUNFIRE_CONFIG:
                isEnabled = GunfireConfig.isEnabled();
                break;
        }
        return Component.translatable(isEnabled ? "button.ezvcsurvival.disable_all" : "button.ezvcsurvival.enable_all");
    }

    public void refreshData() {
        safeRefresh();

        EZVCNetwork.INSTANCE.sendToServer(
                new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND,
                        "refresh", true, 1.0, 1.0)
        );
    }

    public void updateList() {
        list.clear();

        List<Object> filtered = new ArrayList<>();
        for (Object item : items) {
            String searchableText = getSearchableText(item).toLowerCase();
            if (searchQuery.isEmpty() || searchableText.contains(searchQuery)) {
                filtered.add(item);
            }
        }

        for (Object item : filtered) {
            list.addItem(item);
        }
    }

    private String getSearchableText(Object item) {
        if (item instanceof EntityConfigItem) {
            return ((EntityConfigItem) item).getDisplayName() + " " + ((EntityConfigItem) item).getId();
        } else if (item instanceof SoundConfigItem) {
            return ((SoundConfigItem) item).getId();
        } else if (item instanceof EntityReactionItem) {
            return ((EntityReactionItem) item).getId();
        } else if (item instanceof GunfireEntityItem) {
            return ((GunfireEntityItem) item).getId();
        }
        return "";
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query.trim().toLowerCase();
        updateList();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        int titleWidth = this.font.width(this.title);
        int separatorY = 20;
        fill(poseStack, this.width / 2 - titleWidth / 2 - 10, separatorY,
                this.width / 2 + titleWidth / 2 + 10, separatorY + 1, 0x55FFFFFF);

        this.list.render(poseStack, mouseX, mouseY, partialTick);

        super.render(poseStack, mouseX, mouseY, partialTick);

        searchBox.render(poseStack, mouseX, mouseY, partialTick);
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            this.font.draw(poseStack,
                    searchBox.getMessage().getString(),
                    searchBox.x + 4,
                    searchBox.y + 6,
                    0x888888
            );
        }

        int searchLabelY = searchBox.y - 12;
        this.font.draw(poseStack, Component.translatable("gui.ezvcsurvival.search"),
                searchBox.x, searchLabelY, 0xCCCCCC);

        renderFooter(poseStack);

        renderTooltips(poseStack, mouseX, mouseY);
    }

    private void renderFooter(PoseStack poseStack) {
        int footerY = this.height - 30;
        int leftMargin = (this.width - Math.max(this.width - 40, MIN_WIDTH)) / 2;

        Component count = getCountComponent();
        int countWidth = this.font.width(count);
        this.font.draw(poseStack, count, this.width - countWidth - leftMargin, footerY, 0xAAAAAA);

        Component instructions = getInstructionsComponent();
        this.font.draw(poseStack, instructions, leftMargin, footerY, 0xCCCCCC);
    }

    private Component getCountComponent() {
        if (listType == ListType.ENTITY_CONFIG || listType == ListType.GUNFIRE_CONFIG) {
            return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return Component.translatable("gui.ezvcsurvival.sound_count", list.children().size());
            } else {
                return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
            }
        }
        return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
    }

    private Component getInstructionsComponent() {
        if (listType == ListType.ENTITY_CONFIG || listType == ListType.GUNFIRE_CONFIG) {
            return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return Component.translatable("gui.ezvcsurvival.click_to_edit_sound");
            } else {
                return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
            }
        }
        return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (searchBox.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.setScreen(parent != null ? parent : new ConfigEditorScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        searchBox.tick();
    }

    public void onConfigUpdated() {
        loadData();
        updateList();

        switch (listType) {
            case ENTITY_CONFIG:
                EntityVoiceConfig.persist();
                break;
            case GENERAL_SOUNDS_CONFIG:
                GeneralSoundsConfig.persist();
                break;
            case GUNFIRE_CONFIG:
                GunfireConfig.persist();
                break;
        }
    }

    public void safeRefresh() {
        if (this.minecraft != null && this.minecraft.screen == this) {
            loadData();
            updateList();
        }
    }

    public static class EntityConfigItem {
        private final String id;
        private final EntityType<?> type;
        private final EntityVoiceConfig.EntityConfig config;

        public EntityConfigItem(String id, EntityType<?> type, EntityVoiceConfig.EntityConfig config) {
            this.id = id;
            this.type = type;
            this.config = config;
        }

        public String getId() { return id; }
        public EntityVoiceConfig.EntityConfig getConfig() { return config; }
        public String getDisplayName() { return type.getDescription().getString(); }
    }

    public static class SoundConfigItem {
        private final String id;
        private final SoundEvent sound;
        private final GeneralSoundsConfig.SoundEntry config;
        private final boolean isPriority;

        public SoundConfigItem(String id, SoundEvent sound, GeneralSoundsConfig.SoundEntry config, boolean isPriority) {
            this.id = id;
            this.sound = sound;
            this.config = config;
            this.isPriority = isPriority;
        }

        public String getId() { return id; }
        public SoundEvent getSound() { return sound; }
        public GeneralSoundsConfig.SoundEntry getConfig() { return config; }
    }

    public static class EntityReactionItem {
        private final String id;
        private final GeneralSoundsConfig.Reaction reaction;

        public EntityReactionItem(String id, GeneralSoundsConfig.Reaction reaction) {
            this.id = id;
            this.reaction = reaction;
        }

        public String getId() { return id; }
        public GeneralSoundsConfig.Reaction getReaction() { return reaction; }
    }

    public static class GunfireEntityItem {
        private final String id;
        private final GunfireConfig.Reaction reaction;

        public GunfireEntityItem(String id, GunfireConfig.Reaction reaction) {
            this.id = id;
            this.reaction = reaction;
        }

        public String getId() { return id; }
        public GunfireConfig.Reaction getReaction() { return reaction; }
    }

    private void renderTooltips(PoseStack poseStack, int mouseX, int mouseY) {
        if (list == null) return;

        if (!list.isMouseOver(mouseX, mouseY)) {
            return;
        }

        int index = list.getEntryIndexAt(mouseX, mouseY);
        if (index < 0 || index >= list.children().size()) {
            return;
        }

        ConfigListWidget.Entry entry = list.children().get(index);
        if (entry instanceof ConfigListWidget.UniversalEntry universalEntry) {
            universalEntry.renderTooltip(poseStack, mouseX, mouseY);
        }
    }
}