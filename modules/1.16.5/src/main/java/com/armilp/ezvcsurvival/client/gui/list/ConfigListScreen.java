package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.config.GunfireConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.UpdateConfigPacket;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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

    private TextFieldWidget searchBox;
    private String searchQuery = "";
    private Button clearSearchButton;
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
        super(new TranslationTextComponent(getTitleKey(listType)));
        this.listType = listType;
        this.parent = parent;
        this.items = new ArrayList<Object>();

        if (this.listType == ListType.GUNFIRE_CONFIG) {
            this.showingSounds = false;
        } else if (this.listType == ListType.GENERAL_SOUNDS_CONFIG) {
            this.showingSounds = false;
        }
    }

    private static String getTitleKey(ListType type) {
        switch (type) {
            case ENTITY_CONFIG:
                return "screen.ezvcsurvival.entity_config_list";
            case GENERAL_SOUNDS_CONFIG:
                return "screen.ezvcsurvival.general_sounds_config_list";
            case GUNFIRE_CONFIG:
                return "screen.ezvcsurvival.gunfire_config_list";
            default:
                return "screen.ezvcsurvival.config_list";
        }
    }

    @Override
    protected void init() {
        super.init();

        int usableWidth = Math.max(this.width - 40, MIN_WIDTH);
        int leftMargin = (this.width - usableWidth) / 2;

        int topRowY = 20;
        int searchRowY = topRowY + BUTTON_HEIGHT + VERTICAL_SPACING * 2;
        int listStartY = searchRowY + SEARCH_HEIGHT + VERTICAL_SPACING * 3;

        int availableButtonWidth = usableWidth;
        int buttonCount = getButtonCount();
        int buttonWidth = Math.min(MAX_BUTTON_WIDTH, Math.max(MIN_BUTTON_WIDTH,
                (availableButtonWidth - (buttonCount - 1) * HORIZONTAL_SPACING) / buttonCount));

        backButton = this.addButton(new Button(
                leftMargin, topRowY, buttonWidth, BUTTON_HEIGHT,
                new TranslationTextComponent("button.ezvcsurvival.back"),
                b -> Minecraft.getInstance().setScreen(parent != null ? parent : new ConfigEditorScreen())
        ));

        int currentX = leftMargin + buttonWidth + HORIZONTAL_SPACING;

        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            toggleViewButton = this.addButton(new Button(
                    currentX, topRowY, buttonWidth, BUTTON_HEIGHT,
                    new TranslationTextComponent(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"),
                    b -> toggleView()
            ));
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        if (listType == ListType.GENERAL_SOUNDS_CONFIG || listType == ListType.GUNFIRE_CONFIG || listType == ListType.ENTITY_CONFIG) {
            toggleEnabledButton = this.addButton(new Button(
                    currentX, topRowY, buttonWidth, BUTTON_HEIGHT,
                    getToggleEnabledMessage(),
                    b -> toggleEnabled()
            ));
        }

        int searchWidth = Math.min(300, usableWidth - 100);
        searchBox = new TextFieldWidget(this.font, leftMargin, searchRowY, searchWidth, SEARCH_HEIGHT,
                new TranslationTextComponent("textbox.ezvcsurvival.search"));
        searchBox.setResponder(this::onSearchChanged);
        searchBox.setMaxLength(50);
        this.children.add(searchBox);

        clearSearchButton = this.addButton(new Button(
                leftMargin + searchWidth + HORIZONTAL_SPACING, searchRowY, 20, SEARCH_HEIGHT,
                new StringTextComponent("✕"),
                b -> searchBox.setValue("")
        ));

        // Lista sin scroll bar visible
        this.list = new ConfigListWidget(this, this.minecraft, this.width, this.height,
                listStartY, this.height - 40, 28);
        this.children.add(this.list);

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
        items = new ArrayList<Object>();

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
        for (EntityType<?> type : ForgeRegistries.ENTITIES) {
            EntityClassification category = type.getCategory();
            if (category == EntityClassification.MISC) continue;

            String id = Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString();
            EntityVoiceConfig.EntityConfig config = EntityVoiceConfig.get(id);

            if (config == null) {
                config = EntityVoiceConfig.EntityConfig.defaultFor(type);
                EntityVoiceConfig.set(id, config);
            }

            items.add(new EntityConfigItem(id, type, config));
        }
        Collections.sort(items, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((EntityConfigItem) o1).getDisplayName().compareTo(((EntityConfigItem) o2).getDisplayName());
            }
        });
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

        Collections.sort(items, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((SoundConfigItem) o1).getId().compareTo(((SoundConfigItem) o2).getId());
            }
        });
    }

    private void loadGeneralEntityConfigs() {
        items.clear();
        Map<String, GeneralSoundsConfig.Reaction> entityConfigs = GeneralSoundsConfig.getMobReactions();
        if (entityConfigs == null) {
            entityConfigs = Collections.emptyMap();
        }

        for (EntityType<?> type : ForgeRegistries.ENTITIES) {
            EntityClassification category = type.getCategory();
            if (category == EntityClassification.MISC) continue;

            String id = Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString();
            GeneralSoundsConfig.Reaction reaction = entityConfigs.get(id);

            if (reaction == null) {
                reaction = new GeneralSoundsConfig.Reaction(true, 1.0, category == EntityClassification.MONSTER ? 60.0 : 50.0);
                GeneralSoundsConfig.setMobReaction(id, true, 1.0, category == EntityClassification.MONSTER ? 60.0 : 50.0);
            }

            items.add(new EntityReactionItem(id, reaction));
        }

        Collections.sort(items, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((EntityReactionItem) o1).id.compareTo(((EntityReactionItem) o2).id);
            }
        });
    }

    private void loadGunfireEntityConfigs() {
        items.clear();
        Map<String, GunfireConfig.Reaction> entityConfigs = GunfireConfig.getMobReactions();

        for (EntityType<?> type : ForgeRegistries.ENTITIES) {
            EntityClassification category = type.getCategory();
            if (category == EntityClassification.MISC) continue;

            String id = Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString();
            GunfireConfig.Reaction reaction = entityConfigs.get(id);

            if (reaction == null) {
                reaction = new GunfireConfig.Reaction(true, 1.0, category == EntityClassification.MONSTER ? 50.0 : 40.0);
                GunfireConfig.setMobReaction(id, true, 1.0, category == EntityClassification.MONSTER ? 50.0 : 40.0);
            }

            items.add(new GunfireEntityItem(id, reaction));
        }

        Collections.sort(items, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return ((GunfireEntityItem) o1).id.compareTo(((GunfireEntityItem) o2).id);
            }
        });
    }

    private void toggleView() {
        if (listType != ListType.GENERAL_SOUNDS_CONFIG) return;

        showingSounds = !showingSounds;
        toggleViewButton.setMessage(new TranslationTextComponent(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"));

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

    private ITextComponent getToggleEnabledMessage() {
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
        return new TranslationTextComponent(isEnabled ? "button.ezvcsurvival.disable_all" : "button.ezvcsurvival.enable_all");
    }

    // Continúa en la parte 2...
    public void refreshData() {
        safeRefresh();

        EZVCNetwork.INSTANCE.sendToServer(
                new UpdateConfigPacket(UpdateConfigPacket.ConfigType.GENERAL_SOUND,
                        "refresh", true, 1.0, 1.0)
        );
    }

    public void updateList() {
        list.clear();

        List<Object> filtered = new ArrayList<Object>();
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
            return ((EntityReactionItem) item).id;
        } else if (item instanceof GunfireEntityItem) {
            return ((GunfireEntityItem) item).id;
        }
        return "";
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query.trim().toLowerCase();
        updateList();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(matrixStack);

        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        int titleWidth = this.font.width(this.title);
        int separatorY = 20;
        fill(matrixStack, this.width / 2 - titleWidth / 2 - 10, separatorY,
                this.width / 2 + titleWidth / 2 + 10, separatorY + 1, 0x55FFFFFF);

        this.list.render(matrixStack, mouseX, mouseY, partialTick);

        super.render(matrixStack, mouseX, mouseY, partialTick);

        searchBox.render(matrixStack, mouseX, mouseY, partialTick);
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            this.font.draw(matrixStack,
                    searchBox.getMessage().getString(),
                    searchBox.x + 4,
                    searchBox.y + 6,
                    0x888888
            );
        }

        int searchLabelY = searchBox.y - 12;
        this.font.draw(matrixStack, new TranslationTextComponent("gui.ezvcsurvival.search"),
                searchBox.x, searchLabelY, 0xCCCCCC);

        renderFooter(matrixStack);

        renderTooltips(matrixStack, mouseX, mouseY);
    }

    private void renderFooter(MatrixStack matrixStack) {
        int footerY = this.height - 30;
        int leftMargin = (this.width - Math.max(this.width - 40, MIN_WIDTH)) / 2;

        ITextComponent count = getCountComponent();
        int countWidth = this.font.width(count);
        this.font.draw(matrixStack, count, this.width - countWidth - leftMargin, footerY, 0xAAAAAA);

        ITextComponent instructions = getInstructionsComponent();
        this.font.draw(matrixStack, instructions, leftMargin, footerY, 0xCCCCCC);
    }

    private ITextComponent getCountComponent() {
        if (listType == ListType.ENTITY_CONFIG || listType == ListType.GUNFIRE_CONFIG) {
            return new TranslationTextComponent("gui.ezvcsurvival.entity_count", list.children().size());
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return new TranslationTextComponent("gui.ezvcsurvival.sound_count", list.children().size());
            } else {
                return new TranslationTextComponent("gui.ezvcsurvival.entity_count", list.children().size());
            }
        }
        return new TranslationTextComponent("gui.ezvcsurvival.entity_count", list.children().size());
    }

    private ITextComponent getInstructionsComponent() {
        if (listType == ListType.ENTITY_CONFIG || listType == ListType.GUNFIRE_CONFIG) {
            return new TranslationTextComponent("gui.ezvcsurvival.click_to_edit_entity");
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return new TranslationTextComponent("gui.ezvcsurvival.click_to_edit_sound");
            } else {
                return new TranslationTextComponent("gui.ezvcsurvival.click_to_edit_entity");
            }
        }
        return new TranslationTextComponent("gui.ezvcsurvival.click_to_edit_entity");
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

    // Inner classes
    public static class EntityConfigItem {
        private final String id;
        private final EntityType<?> type;
        private final EntityVoiceConfig.EntityConfig config;

        public EntityConfigItem(String id, EntityType<?> type, EntityVoiceConfig.EntityConfig config) {
            this.id = id;
            this.type = type;
            this.config = config;
        }

        public String getId() {
            return id;
        }

        public EntityVoiceConfig.EntityConfig getConfig() {
            return config;
        }

        public String getDisplayName() {
            return type.getDescription().getString();
        }
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

        public String getId() {
            return id;
        }

        public SoundEvent getSound() {
            return sound;
        }

        public GeneralSoundsConfig.SoundEntry getConfig() {
            return config;
        }
    }

    public static class EntityReactionItem {
        public final String id;
        public final GeneralSoundsConfig.Reaction reaction;

        public EntityReactionItem(String id, GeneralSoundsConfig.Reaction reaction) {
            this.id = id;
            this.reaction = reaction;
        }
    }

    public static class GunfireEntityItem {
        public final String id;
        public final GunfireConfig.Reaction reaction;

        public GunfireEntityItem(String id, GunfireConfig.Reaction reaction) {
            this.id = id;
            this.reaction = reaction;
        }
    }

    private void renderTooltips(MatrixStack matrixStack, int mouseX, int mouseY) {
        if (list == null) return;

        if (!list.isMouseOver(mouseX, mouseY)) {
            return;
        }

        int index = list.getEntryIndexAt(mouseX, mouseY);
        if (index < 0 || index >= list.children().size()) {
            return;
        }

        ConfigListWidget.Entry entry = list.children().get(index);
        if (entry instanceof ConfigListWidget.UniversalEntry) {
            ConfigListWidget.UniversalEntry universalEntry = (ConfigListWidget.UniversalEntry) entry;
            universalEntry.renderTooltip(matrixStack, mouseX, mouseY);
        }
    }
}