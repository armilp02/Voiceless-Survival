package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ConfigListWidget extends GuiListExtended {

    private final ConfigListScreen parent;
    private final int width;
    private final int height;
    private static final int MIN_ENTRY_WIDTH = 300;
    private static final int STATUS_AREA_WIDTH = 120;
    private static final int PADDING = 10;

    private List<IGuiListEntry> entries = new ArrayList<IGuiListEntry>();

    public ConfigListWidget(ConfigListScreen parent, Minecraft mc, int width, int height, int top, int bottom, int slotHeight) {
        super(mc, width, height, top, bottom, slotHeight);
        this.parent = parent;
        this.width = width;
        this.height = height;
    }

    public void addItem(Object item) {
        entries.add(new UniversalEntry(item));
    }

    public void clear() {
        entries.clear();
    }

    @Override
    public IGuiListEntry getListEntry(int index) {
        return entries.get(index);
    }

    @Override
    protected int getSize() {
        return entries.size();
    }

    @Override
    public int getListWidth() {
        return Math.max(MIN_ENTRY_WIDTH, this.width - 20);
    }

    @Override
    protected int getScrollBarX() {
        return this.left + this.getListWidth() + 5;
    }

    public class UniversalEntry implements IGuiListEntry {
        private final Object item;

        public UniversalEntry(Object item) {
            this.item = item;
        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            int actualLeft = ConfigListWidget.this.left + 10;
            int actualWidth = ConfigListWidget.this.getListWidth();

            boolean validHover = isSelected && mouseX >= actualLeft && mouseX <= actualLeft + actualWidth;

            if (validHover) {
                Gui.drawRect(actualLeft, y, actualLeft + actualWidth, y + slotHeight, 0x30FFFFFF);
                Gui.drawRect(actualLeft, y, actualLeft + actualWidth, y + 1, 0x60FFFFFF);
                Gui.drawRect(actualLeft, y + slotHeight - 1, actualLeft + actualWidth, y + slotHeight, 0x60FFFFFF);
            }

            if (slotIndex % 2 == 0) {
                Gui.drawRect(actualLeft, y, actualLeft + actualWidth, y + slotHeight, 0x10000000);
            }

            int centerY = y + slotHeight / 2;
            int textLeft = actualLeft + PADDING;
            int statusRight = actualLeft + actualWidth - PADDING;

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                renderEntityConfig(textLeft, y, actualWidth, slotHeight, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                renderSoundConfig(textLeft, y, actualWidth, slotHeight, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                renderEntityReaction(textLeft, y, actualWidth, slotHeight, centerY, statusRight);
            }
        }

        private void renderEntityConfig(int textLeft, int top, int width, int height, int centerY, int statusRight) {
            ConfigListScreen.EntityConfigItem entityItem = (ConfigListScreen.EntityConfigItem) item;
            String entityName = entityItem.getDisplayName();

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(entityName, maxTextWidth);

            Minecraft.getMinecraft().fontRenderer.drawString(displayName, textLeft, centerY - 4, 0xFFFFFF);

            boolean isEnabled = entityItem.getConfig().enabled;
            String statusText = isEnabled
                    ? I18n.format("gui.ezvcsurvival.enabled")
                    : I18n.format("gui.ezvcsurvival.disabled");

            int statusColor = isEnabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(statusText);

            Gui.drawRect(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    isEnabled ? 0x2055FF55 : 0x20FF5555);

            Minecraft.getMinecraft().fontRenderer.drawString(statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor);
        }

        private void renderSoundConfig(int textLeft, int top, int width, int height, int centerY, int statusRight) {
            ConfigListScreen.SoundConfigItem soundItem = (ConfigListScreen.SoundConfigItem) item;
            String soundName = soundItem.getId();

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(soundName, maxTextWidth);

            Minecraft.getMinecraft().fontRenderer.drawString(displayName, textLeft, centerY - 4, 0xFFFFFF);

            boolean isEnabled = soundItem.getConfig().enabled;
            String statusText = isEnabled
                    ? I18n.format("gui.ezvcsurvival.enabled")
                    : I18n.format("gui.ezvcsurvival.disabled");

            int statusColor = isEnabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(statusText);

            Gui.drawRect(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    isEnabled ? 0x2055FF55 : 0x20FF5555);

            Minecraft.getMinecraft().fontRenderer.drawString(statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor);
        }

        private void renderEntityReaction(int textLeft, int top, int width, int height, int centerY, int statusRight) {
            ConfigListScreen.EntityReactionItem entityItem = (ConfigListScreen.EntityReactionItem) item;
            String entityId = entityItem.id;
            String entityName = getEntityDisplayName(entityId);

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(entityName, maxTextWidth);

            Minecraft.getMinecraft().fontRenderer.drawString(displayName, textLeft, centerY - 4, 0xFFFFFF);

            boolean isEnabled = entityItem.reaction.enabled;
            String statusText = isEnabled
                    ? I18n.format("gui.ezvcsurvival.enabled")
                    : I18n.format("gui.ezvcsurvival.disabled");

            int statusColor = isEnabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(statusText);

            Gui.drawRect(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    isEnabled ? 0x2055FF55 : 0x20FF5555);

            Minecraft.getMinecraft().fontRenderer.drawString(statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor);
        }

        private String truncateText(String text, int maxWidth) {
            if (maxWidth <= 0) return text;

            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            int textWidth = fr.getStringWidth(text);

            if (textWidth <= maxWidth) {
                return text;
            }

            String ellipsis = "...";
            int ellipsisWidth = fr.getStringWidth(ellipsis);
            int availableWidth = maxWidth - ellipsisWidth;

            if (availableWidth <= 0) return ellipsis;

            int left = 0;
            int right = text.length();

            while (left < right) {
                int mid = (left + right + 1) / 2;
                String truncated = text.substring(0, mid);

                if (fr.getStringWidth(truncated) <= availableWidth) {
                    left = mid;
                } else {
                    right = mid - 1;
                }
            }

            return text.substring(0, left) + ellipsis;
        }

        private String getEntityDisplayName(String entityId) {
            try {
                ResourceLocation key = new ResourceLocation(entityId);
                if (ForgeRegistries.ENTITIES.containsKey(key)) {
                    EntityEntry entry = ForgeRegistries.ENTITIES.getValue(key);
                    if (entry != null) {
                        return entry.getName();
                    }
                }
            } catch (Exception ignored) {
            }
            return entityId.contains(":") ? entityId.split(":")[1] : entityId;
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            openEditScreen();
            return true;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
        }

        @Override
        public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
        }

        public void renderTooltip(int mouseX, int mouseY) {
            List<String> tooltipLines = getTooltipLines();
            if (tooltipLines.isEmpty()) {
                return;
            }

            FontRenderer font = Minecraft.getMinecraft().fontRenderer;

            int maxLineWidth = 0;
            for (String line : tooltipLines) {
                int w = font.getStringWidth(line);
                if (w > maxLineWidth) {
                    maxLineWidth = w;
                }
            }

            int lineHeight = 10;
            int padding = 8;
            int tooltipHeight = padding + tooltipLines.size() * lineHeight;

            int tooltipX = mouseX + 12;
            int tooltipY = mouseY - 12;

            if (tooltipX + maxLineWidth + padding > ConfigListWidget.this.width) {
                tooltipX = Math.max(8, mouseX - 12 - maxLineWidth - padding);
            }

            if (tooltipY + tooltipHeight > ConfigListWidget.this.height) {
                tooltipY = Math.max(8, ConfigListWidget.this.height - tooltipHeight - 8);
            }

            if (tooltipY < 8) {
                tooltipY = mouseY + 12;
            }

            Gui.drawRect(tooltipX - 3, tooltipY - 4, tooltipX + maxLineWidth + padding, tooltipY + tooltipHeight, 0xF0100010);
            Gui.drawRect(tooltipX - 4, tooltipY - 5, tooltipX + maxLineWidth + padding + 1, tooltipY - 4, 0x505000FF);
            Gui.drawRect(tooltipX - 4, tooltipY + tooltipHeight, tooltipX + maxLineWidth + padding + 1, tooltipY + tooltipHeight + 1, 0x505000FF);
            Gui.drawRect(tooltipX - 4, tooltipY - 4, tooltipX - 3, tooltipY + tooltipHeight, 0x505000FF);
            Gui.drawRect(tooltipX + maxLineWidth + padding, tooltipY - 4, tooltipX + maxLineWidth + padding + 1, tooltipY + tooltipHeight, 0x505000FF);

            for (int i = 0; i < tooltipLines.size(); i++) {
                font.drawString(tooltipLines.get(i), tooltipX, tooltipY + i * lineHeight, 0xFFFFFF);
            }
        }

        private List<String> getTooltipLines() {
            List<String> tooltip = new ArrayList<String>();

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                ConfigListScreen.EntityConfigItem entityItem = (ConfigListScreen.EntityConfigItem) item;
                com.armilp.ezvcsurvival.config.EntityVoiceConfig.EntityConfig config = entityItem.getConfig();

                tooltip.add("§6§l" + entityItem.getDisplayName());
                tooltip.add("§7ID: §f" + entityItem.getId());
                tooltip.add("");
                tooltip.add(I18n.format("tooltip.ezvcsurvival.configuration"));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.enabled",
                        config.enabled ? I18n.format("gui.ezvcsurvival.enabled") : I18n.format("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.speed", "§e" + config.speed));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.range", "§e" + config.range));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.threshold", "§e" + config.threshold));

            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                ConfigListScreen.SoundConfigItem soundItem = (ConfigListScreen.SoundConfigItem) item;
                com.armilp.ezvcsurvival.config.GeneralSoundsConfig.SoundEntry config = soundItem.getConfig();

                tooltip.add("§6§lSound: §f" + soundItem.getId());
                tooltip.add("");
                tooltip.add(I18n.format("tooltip.ezvcsurvival.configuration"));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.enabled",
                        config.enabled ? I18n.format("gui.ezvcsurvival.enabled") : I18n.format("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.speed_multiplier", "§e" + config.speed_multiplier));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.range_multiplier", "§e" + config.range_multiplier));

            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                ConfigListScreen.EntityReactionItem entityItem = (ConfigListScreen.EntityReactionItem) item;
                com.armilp.ezvcsurvival.config.GeneralSoundsConfig.Reaction reaction = entityItem.reaction;
                String entityName = getEntityDisplayName(entityItem.id);

                tooltip.add("§6§l" + entityName);
                tooltip.add("§7ID: §f" + entityItem.id);
                tooltip.add("");
                tooltip.add(I18n.format("tooltip.ezvcsurvival.sound_reaction"));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.enabled",
                        reaction.enabled ? I18n.format("gui.ezvcsurvival.enabled") : I18n.format("gui.ezvcsurvival.disabled")));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.speed", "§e" + reaction.speed));
                tooltip.add(I18n.format("tooltip.ezvcsurvival.range", "§e" + reaction.range));
            }

            return tooltip;
        }

        private void openEditScreen() {
            ConfigEditScreen.EditType editType = null;
            String elementName = "";

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                editType = ConfigEditScreen.EditType.ENTITY_CONFIG;
                elementName = ((ConfigListScreen.EntityConfigItem) item).getDisplayName();
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_CONFIG;
                elementName = ((ConfigListScreen.SoundConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                editType = ConfigEditScreen.EditType.GENERAL_SOUND_ENTITY;
                elementName = getEntityDisplayName(((ConfigListScreen.EntityReactionItem) item).id);
            }

            if (editType != null) {
                String elementId = getElementId();
                Minecraft.getMinecraft().displayGuiScreen(new ConfigEditScreen(parent, editType, elementId, elementName));
            }
        }

        private String getElementId() {
            if (item instanceof ConfigListScreen.EntityConfigItem) {
                return ((ConfigListScreen.EntityConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                return ((ConfigListScreen.SoundConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                return ((ConfigListScreen.EntityReactionItem) item).id;
            }
            return "";
        }
    }

    public int getEntryIndexAt(double mouseX, double mouseY) {
        int left = this.left + 10;
        int listWidth = this.getListWidth();

        for (int i = 0; i < entries.size(); i++) {
            int entryTop = this.top + 4 - (int) this.getAmountScrolled() + i * this.slotHeight + this.headerPadding;
            int entryBottom = entryTop + this.slotHeight;

            if (mouseX >= left && mouseX <= left + listWidth &&
                    mouseY >= entryTop && mouseY <= entryBottom) {
                return i;
            }
        }
        return -1;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        int left = this.left + 10;
        int listWidth = this.getListWidth();

        for (int i = 0; i < entries.size(); i++) {
            int entryTop = this.top + 4 - (int) this.getAmountScrolled() + i * this.slotHeight + this.headerPadding;
            int entryBottom = entryTop + this.slotHeight;

            if (mouseX >= left && mouseX <= left + listWidth &&
                    mouseY >= entryTop && mouseY <= entryBottom) {
                return true;
            }
        }
        return false;
    }


    public List<IGuiListEntry> getEntries() {
        return entries;
    }
}