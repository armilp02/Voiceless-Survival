package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigListWidget extends ObjectSelectionList<ConfigListWidget.Entry> {

    private final ConfigListScreen parent;
    private static final int MIN_ENTRY_WIDTH = 300;
    private static final int STATUS_AREA_WIDTH = 120;
    private static final int PADDING = 10;

    public ConfigListWidget(ConfigListScreen parent, Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, itemHeight);
        this.parent = parent;
    }

    public void addItem(Object item) {
        this.addEntry(new UniversalEntry(item));
    }

    public void clear() {
        this.clearEntries();
    }

    public int getEntryIndexAt(double mouseX, double mouseY) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return -1;
        }

        int rowLeft = this.getRowLeft();
        int rowWidth = this.getRowWidth();

        int count = this.children().size();
        for (int i = 0; i < count; i++) {
            int rowTop = this.getRowTop(i);
            int rowBottom = rowTop + this.itemHeight;

            if (mouseX >= rowLeft && mouseX <= rowLeft + rowWidth &&
                    mouseY >= rowTop && mouseY <= rowBottom) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowWidth() {
        return Math.max(MIN_ENTRY_WIDTH, this.width - 20);
    }

    @Override
    public int getRowLeft() {
        return 10;
    }

    @Override
    protected int scrollBarX() {
        return this.getRowLeft() + this.getRowWidth() + 5;
    }

    public abstract static class Entry extends ObjectSelectionList.Entry<Entry> {
    }

    public class UniversalEntry extends Entry {
        private final Object item;

        public UniversalEntry(Object item) {
            this.item = item;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {

            int actualLeft = ConfigListWidget.this.getRowLeft();
            int actualWidth = ConfigListWidget.this.getRowWidth();

            boolean validHover = hovered && mouseX >= actualLeft && mouseX <= actualLeft + actualWidth;

            if (validHover) {
                graphics.fill(actualLeft, top, actualLeft + actualWidth, top + height, 0x30FFFFFF);
                graphics.fill(actualLeft, top, actualLeft + actualWidth, top + 1, 0x60FFFFFF);
                graphics.fill(actualLeft, top + height - 1, actualLeft + actualWidth, top + height, 0x60FFFFFF);
            }

            if (index % 2 == 0) {
                graphics.fill(actualLeft, top, actualLeft + actualWidth, top + height, 0x10000000);
            }

            int centerY = top + height / 2;
            int textLeft = actualLeft + PADDING;
            int statusRight = actualLeft + actualWidth - PADDING;

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                renderEntityConfig(graphics, textLeft, top, actualWidth, height, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                renderSoundConfig(graphics, textLeft, top, actualWidth, height, centerY, statusRight);
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                renderEntityReaction(graphics, textLeft, top, actualWidth, height, centerY, statusRight);
            }
        }

        private void renderEntityConfig(GuiGraphics graphics, int textLeft, int top, int width, int height,
                                        int centerY, int statusRight) {
            ConfigListScreen.EntityConfigItem entityItem = (ConfigListScreen.EntityConfigItem) item;
            String entityName = entityItem.getDisplayName();

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(entityName, maxTextWidth);

            graphics.drawString(Minecraft.getInstance().font, displayName, textLeft, centerY - 4, 0xFFFFFF, false);

            Component statusText = entityItem.getConfig().enabled
                    ? Component.translatable("gui.ezvcsurvival.enabled")
                    : Component.translatable("gui.ezvcsurvival.disabled");

            int statusColor = entityItem.getConfig().enabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = Minecraft.getInstance().font.width(statusText);

            graphics.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    entityItem.getConfig().enabled ? 0x2055FF55 : 0x20FF5555);

            graphics.drawString(Minecraft.getInstance().font, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private void renderSoundConfig(GuiGraphics graphics, int textLeft, int top, int width, int height,
                                       int centerY, int statusRight) {
            ConfigListScreen.SoundConfigItem soundItem = (ConfigListScreen.SoundConfigItem) item;
            String soundName = soundItem.getId();

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(soundName, maxTextWidth);

            graphics.drawString(Minecraft.getInstance().font, displayName, textLeft, centerY - 4, 0xFFFFFF, false);

            Component statusText = soundItem.getConfig().enabled
                    ? Component.translatable("gui.ezvcsurvival.enabled")
                    : Component.translatable("gui.ezvcsurvival.disabled");

            int statusColor = soundItem.getConfig().enabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = Minecraft.getInstance().font.width(statusText);

            graphics.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    soundItem.getConfig().enabled ? 0x2055FF55 : 0x20FF5555);

            graphics.drawString(Minecraft.getInstance().font, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private void renderEntityReaction(GuiGraphics graphics, int textLeft, int top, int width, int height,
                                          int centerY, int statusRight) {
            ConfigListScreen.EntityReactionItem entityItem = (ConfigListScreen.EntityReactionItem) item;
            String entityId = entityItem.getId();

            String entityName = getEntityDisplayName(entityId);

            int maxTextWidth = width - STATUS_AREA_WIDTH - PADDING * 2;
            String displayName = truncateText(entityName, maxTextWidth);

            graphics.drawString(Minecraft.getInstance().font, displayName, textLeft, centerY - 4, 0xFFFFFF, false);

            Component statusText = entityItem.getReaction().enabled
                    ? Component.translatable("gui.ezvcsurvival.enabled")
                    : Component.translatable("gui.ezvcsurvival.disabled");

            int statusColor = entityItem.getReaction().enabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = Minecraft.getInstance().font.width(statusText);

            graphics.fill(statusRight - statusWidth - 6, centerY - 8, statusRight, centerY + 8,
                    entityItem.getReaction().enabled ? 0x2055FF55 : 0x20FF5555);

            graphics.drawString(Minecraft.getInstance().font, statusText,
                    statusRight - statusWidth - 3, centerY - 4, statusColor, false);
        }

        private String truncateText(String text, int maxWidth) {
            if (maxWidth <= 0) return text;

            int textWidth = Minecraft.getInstance().font.width(text);
            if (textWidth <= maxWidth) {
                return text;
            }

            String ellipsis = "...";
            int ellipsisWidth = Minecraft.getInstance().font.width(ellipsis);
            int availableWidth = maxWidth - ellipsisWidth;

            if (availableWidth <= 0) return ellipsis;

            int left = 0;
            int right = text.length();

            while (left < right) {
                int mid = (left + right + 1) / 2;
                String truncated = text.substring(0, mid);

                if (Minecraft.getInstance().font.width(truncated) <= availableWidth) {
                    left = mid;
                } else {
                    right = mid - 1;
                }
            }

            return text.substring(0, left) + ellipsis;
        }

        private String getEntityDisplayName(String entityId) {
            try {
                ResourceLocation key = ResourceLocation.parse(entityId);
                var holderOptional = BuiltInRegistries.ENTITY_TYPE.get(key);
                if (holderOptional.isPresent()) {
                    EntityType<?> type = holderOptional.get().value();
                    String translationKey = type.getDescriptionId();
                    return Component.translatable(translationKey).getString();
                }
                return key.getPath();
            } catch (Exception ignored) {
                return entityId.contains(":") ? entityId.substring(entityId.indexOf(':') + 1) : entityId;
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                openEditScreen();
                return true;
            }
            return false;
        }

        public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
            List<Component> tooltipLines = getTooltipLines();
            if (tooltipLines.isEmpty()) {
                return;
            }

            var font = Minecraft.getInstance().font;
            int guiW = graphics.guiWidth();
            int guiH = graphics.guiHeight();

            int maxLineWidth = 0;
            for (Component c : tooltipLines) {
                int w = font.width(c);
                if (w > maxLineWidth) {
                    maxLineWidth = w;
                }
            }

            int lineHeight = 10;
            int padding = 8;
            int tooltipHeight = padding + tooltipLines.size() * lineHeight;

            int tooltipX = mouseX + 12;
            int tooltipY = mouseY - 12;

            if (tooltipX + maxLineWidth + padding > guiW) {
                tooltipX = Math.max(8, mouseX - 12 - maxLineWidth - padding);
            }

            if (tooltipY + tooltipHeight > guiH) {
                tooltipY = Math.max(8, guiH - tooltipHeight - 8);
            }

            if (tooltipY < 8) {
                tooltipY = mouseY + 12;
            }

            graphics.renderComponentTooltip(font, tooltipLines, tooltipX, tooltipY);
        }

        private List<Component> getTooltipLines() {
            List<Component> tooltip = new ArrayList<>();

            if (item instanceof ConfigListScreen.EntityConfigItem entityItem) {
                var config = entityItem.getConfig();

                tooltip.add(Component.literal("§6§l" + entityItem.getDisplayName()));
                tooltip.add(Component.literal("§7ID: §f" + entityItem.getId()));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.configuration"));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.enabled",
                        config.enabled ? Component.translatable("gui.ezvcsurvival.enabled") : Component.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.speed", Component.literal("§e" + config.speed)));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.range", Component.literal("§e" + config.range)));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.threshold", Component.literal("§e" + config.threshold)));

            } else if (item instanceof ConfigListScreen.SoundConfigItem soundItem) {
                var config = soundItem.getConfig();

                tooltip.add(Component.literal("§6§lSound: §f" + soundItem.getId()));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.configuration"));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.enabled",
                        config.enabled ? Component.translatable("gui.ezvcsurvival.enabled") : Component.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.speed_multiplier", Component.literal("§e" + config.speed_multiplier)));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.range_multiplier", Component.literal("§e" + config.range_multiplier)));

            } else if (item instanceof ConfigListScreen.EntityReactionItem entityItem) {
                var reaction = entityItem.getReaction();
                String entityName = getEntityDisplayName(entityItem.getId());

                tooltip.add(Component.literal("§6§l" + entityName));
                tooltip.add(Component.literal("§7ID: §f" + entityItem.getId()));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.sound_reaction"));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.enabled",
                        reaction.enabled ? Component.translatable("gui.ezvcsurvival.enabled") : Component.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.speed", Component.literal("§e" + reaction.speed)));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.range", Component.literal("§e" + reaction.range)));
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
                elementName = getEntityDisplayName(((ConfigListScreen.EntityReactionItem) item).getId());
            }

            if (editType != null) {
                String elementId = getElementId();
                Minecraft.getInstance().setScreen(new ConfigEditScreen(parent, editType, elementId, elementName));
            }
        }

        private String getElementId() {
            if (item instanceof ConfigListScreen.EntityConfigItem) {
                return ((ConfigListScreen.EntityConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                return ((ConfigListScreen.SoundConfigItem) item).getId();
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                return ((ConfigListScreen.EntityReactionItem) item).getId();
            }
            return "";
        }

        @Override
        public @NotNull Component getNarration() {
            if (item instanceof ConfigListScreen.EntityConfigItem entityItem) {
                return Component.literal(entityItem.getDisplayName());
            } else if (item instanceof ConfigListScreen.SoundConfigItem soundItem) {
                return Component.literal("Sound: " + soundItem.getId());
            } else if (item instanceof ConfigListScreen.EntityReactionItem entityItem) {
                return Component.literal(getEntityDisplayName(entityItem.getId()));
            }
            return Component.literal("Unknown Item");
        }
    }
}