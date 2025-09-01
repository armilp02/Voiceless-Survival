package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.GunfireConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.UpdateConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private static final int TOP_MARGIN = 25;
    private static final int LEFT_MARGIN = 15;
    private static final int SEARCH_Y = 70;
    private static final int SEARCH_WIDTH = 250;
    private static final int SEARCH_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_SPACING = 8;
    private static final int LIST_TOP_MARGIN = 110;

    public ConfigListScreen(ListType listType, Screen parent) {
        super(Component.translatable(getTitleKey(listType)));
        this.listType = listType;
        this.parent = parent;
        this.items = new ArrayList<>();
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

        backButton = Button.builder(Component.translatable("button.ezvcsurvival.back"), b ->
                Minecraft.getInstance().setScreen(parent != null ? parent : new ConfigEditorScreen())
        ).bounds(LEFT_MARGIN, TOP_MARGIN, 70, BUTTON_HEIGHT).build();
        this.addRenderableWidget(backButton);

        searchBox = new EditBox(this.font, LEFT_MARGIN, SEARCH_Y, SEARCH_WIDTH, SEARCH_HEIGHT,
                Component.translatable("textbox.ezvcsurvival.search"));
        searchBox.setResponder(this::onSearchChanged);
        searchBox.setMaxLength(50);
        this.addRenderableWidget(searchBox);

        clearSearchButton = Button.builder(Component.literal("✕"), b -> searchBox.setValue(""))
                .bounds(LEFT_MARGIN + SEARCH_WIDTH + BUTTON_SPACING, SEARCH_Y, 22, SEARCH_HEIGHT).build();
        this.addRenderableWidget(clearSearchButton);

        refreshButton = Button.builder(Component.translatable("button.ezvcsurvival.refresh"), b -> refreshData())
                .bounds(this.width - 85, TOP_MARGIN, 70, BUTTON_HEIGHT).build();
        this.addRenderableWidget(refreshButton);

        // Botones específicos para ciertos tipos
        if (listType == ListType.GENERAL_SOUNDS_CONFIG || listType == ListType.GUNFIRE_CONFIG) {
            toggleViewButton = Button.builder(
                    Component.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"),
                    b -> toggleView()
            ).bounds(LEFT_MARGIN + 80, TOP_MARGIN + 2, 110, BUTTON_HEIGHT - 4).build();
            this.addRenderableWidget(toggleViewButton);

            toggleEnabledButton = Button.builder(
                    getToggleEnabledMessage(),
                    b -> toggleEnabled()
            ).bounds(LEFT_MARGIN + 200, TOP_MARGIN + 2, 100, BUTTON_HEIGHT - 4).build();
            this.addRenderableWidget(toggleEnabledButton);
        }

        this.list = new ConfigListWidget(this, this.minecraft, this.width, this.height,
                LIST_TOP_MARGIN, this.height - 20, 28);
        this.addWidget(this.list);

        if (items == null || items.isEmpty()) {
            loadData();
        }

        updateList();
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
                if (showingSounds) {
                    loadGunfireSoundConfigs();
                } else {
                    loadGunfireEntityConfigs();
                }
                break;
        }
    }

    private void loadEntityConfigs() {
        items.clear();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;

            String id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
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
        for (SoundEvent sound : BuiltInRegistries.SOUND_EVENT) {
            String id = Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(sound)).toString();
            GeneralSoundsConfig.SoundEntry config = soundConfigs.get(id);

            if (config == null) {
                config = new GeneralSoundsConfig.SoundEntry(true, 1.0, 1.0);
                GeneralSoundsConfig.setSoundEntry(id, true, 1.0, 1.0);
            }

            items.add(new SoundConfigItem(id, sound, config));
        }
        items.sort(Comparator.comparing(item -> ((SoundConfigItem) item).getId()));
    }

    private void loadGeneralEntityConfigs() {
        items.clear();
        Map<String, GeneralSoundsConfig.Reaction> entityConfigs = GeneralSoundsConfig.getMobReactions();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;

            String id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
            GeneralSoundsConfig.Reaction reaction = entityConfigs.get(id);

            if (reaction == null) {
                reaction = new GeneralSoundsConfig.Reaction(true, 1.0, category == MobCategory.MONSTER ? 60.0 : 50.0);
                GeneralSoundsConfig.setMobReaction(id, true, 1.0, category == MobCategory.MONSTER ? 60.0 : 50.0);
            }

            items.add(new EntityReactionItem(id, reaction));
        }

        items.sort(Comparator.comparing(item -> ((EntityReactionItem) item).getId()));
    }

    private void loadGunfireSoundConfigs() {
        items.clear();
        Map<String, Boolean> soundConfigs = GunfireConfig.getGunPrioritySounds();
        for (SoundEvent sound : BuiltInRegistries.SOUND_EVENT) {
            String id = Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(sound)).toString();
            Boolean config = soundConfigs.get(id);

            if (config == null) {
                config = false;
                GunfireConfig.setPrioritySound(id, false);
            }

            items.add(new GunfireSoundItem(id, sound, config));
        }
        items.sort(Comparator.comparing(item -> ((GunfireSoundItem) item).getId()));
    }

    private void loadGunfireEntityConfigs() {
        items.clear();
        Map<String, GunfireConfig.Reaction> entityConfigs = GunfireConfig.getMobReactions();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;

            String id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
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
        showingSounds = !showingSounds;
        toggleViewButton.setMessage(Component.translatable(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"));
        safeRefresh();
    }

    private void toggleEnabled() {
        switch (listType) {
            case GENERAL_SOUNDS_CONFIG:
                boolean newGeneralState = !GeneralSoundsConfig.isEnabled();
                GeneralSoundsConfig.ROOT.enabled = newGeneralState;
                GeneralSoundsConfig.persist();

                try {
                    EZVCNetwork.INSTANCE.sendToServer(
                            new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND,
                                    "global", newGeneralState, 1.0, 1.0)
                    );
                } catch (Exception ignored) {
                }
                break;

            case GUNFIRE_CONFIG:
                boolean newGunfireState = !GunfireConfig.isEnabled();
                GunfireConfig.ROOT.enabled = newGunfireState;
                GunfireConfig.persist();

                try {
                    EZVCNetwork.INSTANCE.sendToServer(
                            new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GUNFIRE_SOUND,
                                    "global", newGunfireState, 1.0, 1.0)
                    );
                } catch (Exception ignored) {
                }
                break;
        }
        toggleEnabledButton.setMessage(getToggleEnabledMessage());
    }

    private Component getToggleEnabledMessage() {
        boolean isEnabled = false;
        switch (listType) {
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

        try {
            EZVCNetwork.INSTANCE.sendToServer(
                    new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND,
                            "refresh", true, 1.0, 1.0)
            );
        } catch (Exception ignored) {
        }
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
        } else if (item instanceof GunfireSoundItem) {
            return ((GunfireSoundItem) item).getId();
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, TOP_MARGIN + 5, 0xFFFFFF);

        int titleWidth = this.font.width(this.title);
        int separatorY = TOP_MARGIN + 25;
        graphics.fill(this.width / 2 - titleWidth / 2 - 10, separatorY,
                this.width / 2 + titleWidth / 2 + 10, separatorY + 1, 0x55FFFFFF);

        this.list.render(graphics, mouseX, mouseY, partialTick);

        super.render(graphics, mouseX, mouseY, partialTick);

        searchBox.render(graphics, mouseX, mouseY, partialTick);
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            graphics.drawString(
                    this.font,
                    searchBox.getMessage().getString(),
                    searchBox.getX() + 6,
                    searchBox.getY() + 7,
                    0x888888,
                    false
            );
        }

        graphics.drawString(this.font, Component.translatable("gui.ezvcsurvival.search"), LEFT_MARGIN, SEARCH_Y - 15, 0xCCCCCC, false);

        int bottomY = this.height - 30;
        Component count = getCountComponent();
        int countWidth = this.font.width(count);
        graphics.drawString(this.font, count, this.width - countWidth - 15, bottomY, 0xAAAAAA, false);

        Component instructions = getInstructionsComponent();
        graphics.drawString(this.font, instructions, LEFT_MARGIN, bottomY - 15, 0xCCCCCC, false);

        renderTooltips(graphics, mouseX, mouseY);
    }

    private Component getCountComponent() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
        } else if (showingSounds) {
            return Component.translatable("gui.ezvcsurvival.sound_count", list.children().size());
        } else {
            return Component.translatable("gui.ezvcsurvival.entity_count", list.children().size());
        }
    }

    private Component getInstructionsComponent() {
        if (listType == ListType.ENTITY_CONFIG) {
            return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
        } else if (showingSounds) {
            if (listType == ListType.GUNFIRE_CONFIG) {
                return Component.translatable("gui.ezvcsurvival.click_to_edit_priority");
            } else {
                return Component.translatable("gui.ezvcsurvival.click_to_edit_sound");
            }
        } else {
            return Component.translatable("gui.ezvcsurvival.click_to_edit_entity");
        }
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
        public EntityType<?> getType() { return type; }
        public EntityVoiceConfig.EntityConfig getConfig() { return config; }
        public String getDisplayName() { return type.getDescription().getString(); }
    }

    public static class SoundConfigItem {
        private final String id;
        private final SoundEvent sound;
        private final GeneralSoundsConfig.SoundEntry config;

        public SoundConfigItem(String id, SoundEvent sound, GeneralSoundsConfig.SoundEntry config) {
            this.id = id;
            this.sound = sound;
            this.config = config;
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

    public static class GunfireSoundItem {
        private final String id;
        private final SoundEvent sound;
        private final boolean priority;

        public GunfireSoundItem(String id, SoundEvent sound, boolean priority) {
            this.id = id;
            this.sound = sound;
            this.priority = priority;
        }

        public String getId() { return id; }
        public SoundEvent getSound() { return sound; }
        public boolean isPriority() { return priority; }
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

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
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
            universalEntry.renderTooltip(graphics, mouseX, mouseY);
        }
    }
}