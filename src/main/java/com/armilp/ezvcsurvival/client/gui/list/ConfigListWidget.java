package com.armilp.ezvcsurvival.client.gui.list;

import com.armilp.ezvcsurvival.client.gui.edit.ConfigEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class ConfigListWidget extends ObjectSelectionList<ConfigListWidget.Entry> {
    
    private final ConfigListScreen parent;

    public ConfigListWidget(ConfigListScreen parent, Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, bottom, itemHeight);
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

            int contentLeft = ConfigListWidget.this.x0 + 15;
            int contentRight = ConfigListWidget.this.x1 - 25;
            boolean validHover = hovered && mouseX >= contentLeft && mouseX <= contentRight;

            if (validHover) {
                graphics.fill(left, top, left + width, top + height, 0x40FFFFFF);
                graphics.fill(left, top, left + width, top + height, 0x20FFFFFF);
            }

            int leftMargin = 15;
            int rightMargin = 15;
            int centerY = top + height / 2;

            if (item instanceof ConfigListScreen.EntityConfigItem) {
                renderEntityConfig(graphics, left, top, width, height, leftMargin, rightMargin, centerY);
            } else if (item instanceof ConfigListScreen.SoundConfigItem) {
                renderSoundConfig(graphics, left, top, width, height, leftMargin, rightMargin, centerY);
            } else if (item instanceof ConfigListScreen.EntityReactionItem) {
                renderEntityReaction(graphics, left, top, width, height, leftMargin, rightMargin, centerY);
            } else if (item instanceof ConfigListScreen.GunfireSoundItem) {
                renderGunfireSound(graphics, left, top, width, height, leftMargin, rightMargin, centerY);
            } else if (item instanceof ConfigListScreen.GunfireEntityItem) {
                renderGunfireEntity(graphics, left, top, width, height, leftMargin, rightMargin, centerY);
            }
        }

        private void renderEntityConfig(GuiGraphics graphics, int left, @SuppressWarnings("unused") int top, int width, @SuppressWarnings("unused") int height, 
                                      int leftMargin, int rightMargin, int centerY) {
            ConfigListScreen.EntityConfigItem entityItem = (ConfigListScreen.EntityConfigItem) item;
            String entityName = entityItem.getDisplayName();

            graphics.drawString(Minecraft.getInstance().font, entityName, left + leftMargin, centerY - 4, 0xFFFFFF, false);

            Component statusText = entityItem.getConfig().enabled
                    ? Component.translatable("gui.ezvcsurvival.enabled")
                    : Component.translatable("gui.ezvcsurvival.disabled");

            int statusColor = entityItem.getConfig().enabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = Minecraft.getInstance().font.width(statusText);

            graphics.drawString(Minecraft.getInstance().font, statusText, 
                              left + width - statusWidth - rightMargin, centerY - 4, statusColor, false);
        }

        private void renderSoundConfig(GuiGraphics graphics, int left, @SuppressWarnings("unused") int top, int width, @SuppressWarnings("unused") int height, 
                                     int leftMargin, int rightMargin, int centerY) {
            ConfigListScreen.SoundConfigItem soundItem = (ConfigListScreen.SoundConfigItem) item;
            String soundName = soundItem.getId();

            graphics.drawString(Minecraft.getInstance().font, soundName, left + leftMargin, centerY - 4, 0xFFFFFF, false);

            String statusSymbol = soundItem.getConfig().enabled ? "✔" : "✖";
            int statusColor = soundItem.getConfig().enabled ? 0x55FF55 : 0xFF5555;
            int symbolWidth = Minecraft.getInstance().font.width(statusSymbol);

            graphics.drawString(Minecraft.getInstance().font, statusSymbol, 
                              left + width - symbolWidth - rightMargin, centerY - 4, statusColor, false);
        }

        private void renderEntityReaction(GuiGraphics graphics, int left, @SuppressWarnings("unused") int top, int width, @SuppressWarnings("unused") int height, 
                                        int leftMargin, int rightMargin, int centerY) {
            ConfigListScreen.EntityReactionItem entityItem = (ConfigListScreen.EntityReactionItem) item;
            String entityId = entityItem.getId();

            String entityName = getEntityDisplayName(entityId);

            graphics.drawString(Minecraft.getInstance().font, entityName, left + leftMargin, centerY - 4, 0xFFFFFF, false);

            Component statusText = entityItem.getReaction().enabled
                    ? Component.translatable("gui.ezvcsurvival.enabled")
                    : Component.translatable("gui.ezvcsurvival.disabled");

            int statusColor = entityItem.getReaction().enabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = Minecraft.getInstance().font.width(statusText);

            graphics.drawString(Minecraft.getInstance().font, statusText, 
                              left + width - statusWidth - rightMargin, centerY - 4, statusColor, false);
        }

        private void renderGunfireSound(GuiGraphics graphics, int left, @SuppressWarnings("unused") int top, int width, @SuppressWarnings("unused") int height, 
                                       int leftMargin, int rightMargin, int centerY) {
            ConfigListScreen.GunfireSoundItem soundItem = (ConfigListScreen.GunfireSoundItem) item;
            String soundName = soundItem.getId();

            graphics.drawString(Minecraft.getInstance().font, soundName, left + leftMargin, centerY - 4, 0xFFFFFF, false);

            String prioritySymbol = soundItem.isPriority() ? "✔" : "✖";
            int priorityColor = soundItem.isPriority() ? 0x55FF55 : 0xFF5555;
            int symbolWidth = Minecraft.getInstance().font.width(prioritySymbol);

            graphics.drawString(Minecraft.getInstance().font, prioritySymbol, 
                              left + width - symbolWidth - rightMargin, centerY - 4, priorityColor, false);
        }

        private void renderGunfireEntity(GuiGraphics graphics, int left, @SuppressWarnings("unused") int top, int width, @SuppressWarnings("unused") int height, 
                                        int leftMargin, int rightMargin, int centerY) {
            ConfigListScreen.GunfireEntityItem entityItem = (ConfigListScreen.GunfireEntityItem) item;
            String entityId = entityItem.getId();

            String entityName = getEntityDisplayName(entityId);

            graphics.drawString(Minecraft.getInstance().font, entityName, left + leftMargin, centerY - 4, 0xFFFFFF, false);

            Component statusText = entityItem.getReaction().enabled
                    ? Component.translatable("gui.ezvcsurvival.enabled")
                    : Component.translatable("gui.ezvcsurvival.disabled");

            int statusColor = entityItem.getReaction().enabled ? 0x55FF55 : 0xFF5555;
            int statusWidth = Minecraft.getInstance().font.width(statusText);

            graphics.drawString(Minecraft.getInstance().font, statusText, 
                              left + width - statusWidth - rightMargin, centerY - 4, statusColor, false);
        }

        @SuppressWarnings("deprecation")
        private String getEntityDisplayName(String entityId) {
            try {
                var key = new ResourceLocation(entityId);
                var registry = BuiltInRegistries.ENTITY_TYPE;
                if (registry.containsKey(key)) {
                    var entityType = registry.get(key);
                    return entityType.getDescription().getString();
                }
            } catch (Exception ignored) {}

            return entityId.contains(":") ? entityId.split(":")[1] : entityId;
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
            int tooltipY = mouseY + 4;

            if (tooltipX + maxLineWidth + padding > guiW) {
                tooltipX = Math.max(8, mouseX - 12 - maxLineWidth - padding);
            }

            if (tooltipY + tooltipHeight > guiH) {
                tooltipY = Math.max(8, guiH - tooltipHeight - 8);
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

            } else if (item instanceof ConfigListScreen.GunfireSoundItem soundItem) {
                tooltip.add(Component.literal("§6§lGunfire Sound: §f" + soundItem.getId()));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.configuration"));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.high_priority",
                        soundItem.isPriority() ? Component.translatable("gui.ezvcsurvival.enabled") : Component.translatable("gui.ezvcsurvival.disabled")));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.priority_explanation"));

            } else if (item instanceof ConfigListScreen.GunfireEntityItem entityItem) {
                var reaction = entityItem.getReaction();
                String entityName = getEntityDisplayName(entityItem.getId());

                tooltip.add(Component.literal("§6§l" + entityName));
                tooltip.add(Component.literal("§7ID: §f" + entityItem.getId()));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("tooltip.ezvcsurvival.gunfire_reaction"));
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
            } else if (item instanceof ConfigListScreen.GunfireSoundItem) {
                editType = ConfigEditScreen.EditType.GUNFIRE_SOUND_CONFIG;
                elementName = ((ConfigListScreen.GunfireSoundItem) item).getId();
            } else if (item instanceof ConfigListScreen.GunfireEntityItem) {
                editType = ConfigEditScreen.EditType.GUNFIRE_ENTITY;
                elementName = getEntityDisplayName(((ConfigListScreen.GunfireEntityItem) item).getId());
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
            } else if (item instanceof ConfigListScreen.GunfireSoundItem) {
                return ((ConfigListScreen.GunfireSoundItem) item).getId();
            } else if (item instanceof ConfigListScreen.GunfireEntityItem) {
                return ((ConfigListScreen.GunfireEntityItem) item).getId();
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
            } else if (item instanceof ConfigListScreen.GunfireSoundItem soundItem) {
                return Component.literal("Gunfire Sound: " + soundItem.getId());
            } else if (item instanceof ConfigListScreen.GunfireEntityItem entityItem) {
                return Component.literal(getEntityDisplayName(entityItem.getId()));
            }
            return Component.literal("Unknown Item");
        }
    }
}

