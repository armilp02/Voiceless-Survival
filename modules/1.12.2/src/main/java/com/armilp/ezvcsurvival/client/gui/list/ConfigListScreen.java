package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.ConfigEditorScreen;
import com.armilp.ezvcsurvival.config.EntityVoiceConfig;
import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.armilp.ezvcsurvival.network.EZVCNetwork;
import com.armilp.ezvcsurvival.network.UpdateConfigPacket;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.IOException;
import java.util.*;

public class ConfigListScreen extends GuiScreen {

    public enum ListType {
        ENTITY_CONFIG,
        GENERAL_SOUNDS_CONFIG
    }

    private final ListType listType;
    private final GuiScreen parent;

    private GuiTextField searchBox;
    private String searchQuery = "";
    private GuiButton clearSearchButton;
    private GuiButton backButton;
    private GuiButton toggleViewButton;
    private GuiButton toggleEnabledButton;
    private ConfigListWidget list;

    private List<Object> items;
    private boolean showingSounds = true;

    private static final int MIN_WIDTH = 400;
    private static final int MIN_BUTTON_WIDTH = 60;
    private static final int MAX_BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SEARCH_HEIGHT = 20;
    private static final int VERTICAL_SPACING = 6;
    private static final int HORIZONTAL_SPACING = 6;

    public ConfigListScreen(ListType listType, GuiScreen parent) {
        this.listType = listType;
        this.parent = parent;
        this.items = new ArrayList<Object>();

        if (this.listType == ListType.GENERAL_SOUNDS_CONFIG) {
            this.showingSounds = false;
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        int usableWidth = Math.max(this.width - 40, MIN_WIDTH);
        int leftMargin = (this.width - usableWidth) / 2;

        int topRowY = 20;
        int searchRowY = topRowY + BUTTON_HEIGHT + VERTICAL_SPACING * 2;
        int listStartY = searchRowY + SEARCH_HEIGHT + VERTICAL_SPACING * 3;

        int availableButtonWidth = usableWidth;
        int buttonCount = getButtonCount();
        int buttonWidth = Math.min(MAX_BUTTON_WIDTH, Math.max(MIN_BUTTON_WIDTH,
                (availableButtonWidth - (buttonCount - 1) * HORIZONTAL_SPACING) / buttonCount));

        backButton = new GuiButton(0, leftMargin, topRowY, buttonWidth, BUTTON_HEIGHT,
                I18n.format("button.ezvcsurvival.back"));
        this.buttonList.add(backButton);

        int currentX = leftMargin + buttonWidth + HORIZONTAL_SPACING;

        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            toggleViewButton = new GuiButton(1, currentX, topRowY, buttonWidth, BUTTON_HEIGHT,
                    I18n.format(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds"));
            this.buttonList.add(toggleViewButton);
            currentX += buttonWidth + HORIZONTAL_SPACING;
        }

        if (listType == ListType.GENERAL_SOUNDS_CONFIG || listType == ListType.ENTITY_CONFIG) {
            toggleEnabledButton = new GuiButton(2, currentX, topRowY, buttonWidth, BUTTON_HEIGHT,
                    getToggleEnabledMessage());
            this.buttonList.add(toggleEnabledButton);
        }

        int searchWidth = Math.min(300, usableWidth - 100);
        searchBox = new GuiTextField(0, this.fontRenderer, leftMargin, searchRowY, searchWidth, SEARCH_HEIGHT);
        searchBox.setMaxStringLength(50);

        clearSearchButton = new GuiButton(3, leftMargin + searchWidth + HORIZONTAL_SPACING, searchRowY, 20, SEARCH_HEIGHT, "✕");
        this.buttonList.add(clearSearchButton);

        this.list = new ConfigListWidget(this, this.mc, this.width, this.height,
                listStartY, this.height - 40, 28);

        if (items == null || items.isEmpty()) {
            loadData();
        }

        updateList();
    }

    private int getButtonCount() {
        int count = 2;
        if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            count += 2;
        } else if (listType == ListType.ENTITY_CONFIG) {
            count += 1;
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
        }
    }

    private void loadEntityConfigs() {
        items.clear();
        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValues()) {
            Class<?> entityClass = entry.getEntityClass();
            if (!EntityLiving.class.isAssignableFrom(entityClass)) continue;

            String id = entry.getRegistryName().toString();
            EntityVoiceConfig.EntityConfig config = EntityVoiceConfig.get(id);

            if (config == null) {
                config = EntityVoiceConfig.EntityConfig.defaultFor(entry);
                EntityVoiceConfig.set(id, config);
            }

            items.add(new EntityConfigItem(id, entry, config));
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
            ResourceLocation key = sound.getRegistryName();
            if (key == null) continue;
            String id = key.toString();

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

        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValues()) {
            Class<?> entityClass = entry.getEntityClass();
            if (!EntityLiving.class.isAssignableFrom(entityClass)) continue;

            String id = entry.getRegistryName().toString();
            GeneralSoundsConfig.Reaction reaction = entityConfigs.get(id);

            if (reaction == null) {
                boolean isMonster = isMonsterType(entry);
                reaction = new GeneralSoundsConfig.Reaction(true, 1.0, isMonster ? 60.0 : 50.0);
                GeneralSoundsConfig.setMobReaction(id, true, 1.0, isMonster ? 60.0 : 50.0);
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

    private boolean isMonsterType(EntityEntry entry) {
        try {
            Class<?> entityClass = entry.getEntityClass();

            if (EntityMob.class.isAssignableFrom(entityClass)) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void toggleView() {
        if (listType != ListType.GENERAL_SOUNDS_CONFIG) return;

        showingSounds = !showingSounds;
        toggleViewButton.displayString = I18n.format(showingSounds ? "button.ezvcsurvival.show_entities" : "button.ezvcsurvival.show_sounds");

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
        }
        toggleEnabledButton.displayString = getToggleEnabledMessage();
        safeRefresh();
    }

    private String getToggleEnabledMessage() {
        boolean isEnabled = false;
        switch (listType) {
            case ENTITY_CONFIG:
                isEnabled = EntityVoiceConfig.isEnabled();
                break;
            case GENERAL_SOUNDS_CONFIG:
                isEnabled = GeneralSoundsConfig.isEnabled();
                break;
        }
        return I18n.format(isEnabled ? "button.ezvcsurvival.disable_all" : "button.ezvcsurvival.enable_all");
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
        }
        return "";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(parent != null ? parent : new ConfigEditorScreen());
        } else if (button.id == 1) {
            toggleView();
        } else if (button.id == 2) {
            toggleEnabled();
        } else if (button.id == 3) {
            searchBox.setText("");
            searchQuery = "";
            updateList();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        String title = getTitleKey(listType);
        this.drawCenteredString(this.fontRenderer, title, this.width / 2, 8, 0xFFFFFF);

        int titleWidth = this.fontRenderer.getStringWidth(title);
        int separatorY = 20;
        this.drawHorizontalLine(this.width / 2 - titleWidth / 2 - 10, this.width / 2 + titleWidth / 2 + 10, separatorY, 0x55FFFFFF);

        this.list.drawScreen(mouseX, mouseY, partialTicks);

        super.drawScreen(mouseX, mouseY, partialTicks);

        searchBox.drawTextBox();
        if (searchBox.getText().isEmpty() && !searchBox.isFocused()) {
            this.fontRenderer.drawString(I18n.format("textbox.ezvcsurvival.search"),
                    searchBox.x + 4, searchBox.y + 6, 0x888888);
        }

        int searchLabelY = searchBox.y - 12;
        this.fontRenderer.drawString(I18n.format("gui.ezvcsurvival.search"),
                searchBox.x, searchLabelY, 0xCCCCCC);

        renderFooter();

        renderTooltips(mouseX, mouseY);
    }

    private String getTitleKey(ListType type) {
        switch (type) {
            case ENTITY_CONFIG:
                return I18n.format("screen.ezvcsurvival.entity_config_list");
            case GENERAL_SOUNDS_CONFIG:
                return I18n.format("screen.ezvcsurvival.general_sounds_config_list");
            default:
                return I18n.format("screen.ezvcsurvival.config_list");
        }
    }

    private void renderFooter() {
        int footerY = this.height - 30;
        int leftMargin = (this.width - Math.max(this.width - 40, MIN_WIDTH)) / 2;

        String count = getCountString();
        int countWidth = this.fontRenderer.getStringWidth(count);
        this.fontRenderer.drawString(count, this.width - countWidth - leftMargin, footerY, 0xAAAAAA);

        String instructions = getInstructionsString();
        this.fontRenderer.drawString(instructions, leftMargin, footerY, 0xCCCCCC);
    }

    private String getCountString() {
        if (listType == ListType.ENTITY_CONFIG) {
            return I18n.format("gui.ezvcsurvival.entity_count", list.getEntries().size());
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return I18n.format("gui.ezvcsurvival.sound_count", list.getEntries().size());
            } else {
                return I18n.format("gui.ezvcsurvival.entity_count", list.getEntries().size());
            }
        }
        return I18n.format("gui.ezvcsurvival.entity_count", list.getEntries().size());
    }

    private String getInstructionsString() {
        if (listType == ListType.ENTITY_CONFIG) {
            return I18n.format("gui.ezvcsurvival.click_to_edit_entity");
        } else if (listType == ListType.GENERAL_SOUNDS_CONFIG) {
            if (showingSounds) {
                return I18n.format("gui.ezvcsurvival.click_to_edit_sound");
            } else {
                return I18n.format("gui.ezvcsurvival.click_to_edit_entity");
            }
        }
        return I18n.format("gui.ezvcsurvival.click_to_edit_entity");
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchBox.textboxKeyTyped(typedChar, keyCode)) {
            searchQuery = searchBox.getText().trim().toLowerCase();
            updateList();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchBox.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        searchBox.updateCursorCounter();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.list.handleMouseInput();
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
        }
    }

    public void safeRefresh() {
        if (this.mc != null && this.mc.currentScreen == this) {
            loadData();
            updateList();
        }
    }

    public static class EntityConfigItem {
        private final String id;
        private final EntityEntry entry;
        private final EntityVoiceConfig.EntityConfig config;

        public EntityConfigItem(String id, EntityEntry entry, EntityVoiceConfig.EntityConfig config) {
            this.id = id;
            this.entry = entry;
            this.config = config;
        }

        public String getId() {
            return id;
        }

        public EntityVoiceConfig.EntityConfig getConfig() {
            return config;
        }

        public String getDisplayName() {
            return entry.getName();
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

    private void renderTooltips(int mouseX, int mouseY) {
        if (list == null) return;

        if (!list.isMouseOver(mouseX, mouseY)) {
            return;
        }

        int index = list.getEntryIndexAt(mouseX, mouseY);
        if (index < 0 || index >= list.getEntries().size()) {
            return;
        }

        Object entry = list.getEntries().get(index);
        if (entry instanceof ConfigListWidget.UniversalEntry) {
            ConfigListWidget.UniversalEntry universalEntry = (ConfigListWidget.UniversalEntry) entry;
            universalEntry.renderTooltip(mouseX, mouseY);
        }
    }
}