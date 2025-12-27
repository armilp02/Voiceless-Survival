package com.armilp.ezvcsurvival.client.gui.edit;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SoundFilterEditScreen extends Screen {
    private final Screen parent;
    private final String entityId;
    private final String entityName;

    private EditBox searchBox;
    private Button saveButton;
    private Button backButton;
    private Button clearButton;

    private List<String> blockedSounds;
    private List<SuggestionEntry> suggestions;
    private int scrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private int dragStartY = 0;
    private int dragStartOffset = 0;
    private boolean showOnlyBlocked = false;
    private Button toggleFilterButton;

    private static final int MAX_VISIBLE_SUGGESTIONS = 15;
    private static final int FIELD_WIDTH = 500;
    private static final int FIELD_HEIGHT = 20;
    private static final int SUGGESTION_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 10;

    public SoundFilterEditScreen(Screen parent, String entityId, String entityName) {
        super(Component.literal("Sound Filters: " + entityName));
        this.parent = parent;
        this.entityId = entityId;
        this.entityName = entityName;
        loadCurrentFilters();
        loadSuggestions();
    }

    private void loadCurrentFilters() {
        GeneralSoundsConfig.Reaction reaction = GeneralSoundsConfig.getMobReactions().get(entityId);
        if (reaction != null && reaction.blocked_sounds != null) {
            this.blockedSounds = new ArrayList<>(reaction.blocked_sounds);
        } else {
            this.blockedSounds = new ArrayList<>();
        }
    }

    private void loadSuggestions() {
        suggestions = new ArrayList<>();

        ForgeRegistries.SOUND_EVENTS.forEach(sound -> {
            ResourceLocation key = ForgeRegistries.SOUND_EVENTS.getKey(sound);
            if (key != null) {
                String soundId = key.toString();
                String category = categorizeSound(soundId);
                suggestions.add(new SuggestionEntry(soundId, category));
            }
        });

        suggestions.sort((a, b) -> {
            int catCompare = a.category.compareTo(b.category);
            return catCompare != 0 ? catCompare : a.soundId.compareTo(b.soundId);
        });
    }

    private String categorizeSound(String soundId) {
        String lower = soundId.toLowerCase();

        if (lower.contains("explosion") || lower.contains("explode")) return "Explosions";
        if (lower.contains("gun") || lower.contains("shoot") || lower.contains("fire")) return "Gunfire";
        if (lower.contains("place") || lower.contains("break") || lower.contains("hit")) return "Blocks";
        if (lower.contains("step") || lower.contains("walk")) return "Movement";
        if (lower.contains("ambient") || lower.contains("idle")) return "Ambient";
        if (lower.contains("hurt") || lower.contains("death")) return "Entity Sounds";
        if (lower.contains("music")) return "Music";
        if (lower.contains("weather") || lower.contains("rain") || lower.contains("thunder")) return "Weather";

        if (soundId.startsWith("minecraft:")) return "Minecraft";
        if (soundId.contains(":")) return soundId.split(":")[0].toUpperCase();

        return "Other";
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 40;

        searchBox = new EditBox(this.font, centerX - FIELD_WIDTH / 2, startY, FIELD_WIDTH - 110, FIELD_HEIGHT,
                Component.literal("Search Sounds"));
        searchBox.setResponder(this::onSearchChanged);
        searchBox.setMaxLength(100);
        this.addRenderableWidget(searchBox);

        toggleFilterButton = new Button(centerX + FIELD_WIDTH / 2 - 100, startY, 100, FIELD_HEIGHT,
                Component.literal("Show Blocked"), b -> toggleShowBlocked());
        this.addRenderableWidget(toggleFilterButton);

        int buttonY = this.height - 30;

        clearButton = new Button(centerX - 210, buttonY, 100, 20, Component.literal("Clear All"), b -> clearFilters());
        this.addRenderableWidget(clearButton);

        saveButton = new Button(centerX - 100, buttonY, 100, 20, Component.literal("Save"), b -> saveFilters());
        this.addRenderableWidget(saveButton);

        backButton = new Button(centerX + 10, buttonY, 100, 20, Component.literal("Cancel"),
                b -> Minecraft.getInstance().setScreen(parent));
        this.addRenderableWidget(backButton);
    }

    private void onSearchChanged(String query) {
        scrollOffset = 0;
    }

    private void toggleShowBlocked() {
        showOnlyBlocked = !showOnlyBlocked;
        scrollOffset = 0;
        toggleFilterButton.setMessage(Component.literal(showOnlyBlocked ? "Show All" : "Show Blocked"));
    }

    private void clearFilters() {
        blockedSounds.clear();
    }

    private void saveFilters() {
        GeneralSoundsConfig.Reaction reaction = GeneralSoundsConfig.getMobReactions().get(entityId);
        if (reaction != null) {
            reaction.blocked_sounds = new ArrayList<>(blockedSounds);
            GeneralSoundsConfig.persist();
        }

        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 15, 0xFFFFFF);

        this.font.draw(poseStack, "Search and click to block/unblock sounds:",
                this.width / 2 - FIELD_WIDTH / 2, 25, 0xFFFFFF);

        renderSuggestions(poseStack, mouseX, mouseY);

        drawCenteredString(poseStack, this.font, "§7Blocked: " + blockedSounds.size() + " sounds",
                this.width / 2, this.height - 50, 0xAAAAAA);

        super.render(poseStack, mouseX, mouseY, partialTick);

        renderSuggestionTooltip(poseStack, mouseX, mouseY);
    }

    private void renderSuggestions(PoseStack poseStack, int mouseX, int mouseY) {
        String query = searchBox.getValue().toLowerCase().trim();

        List<SuggestionEntry> filtered = suggestions.stream()
                .filter(s -> {
                    if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                        return false;
                    }
                    return query.isEmpty() ||
                            s.soundId.toLowerCase().contains(query) ||
                            s.category.toLowerCase().contains(query);
                })
                .collect(Collectors.toList());

        int startX = this.width / 2 - FIELD_WIDTH / 2;
        int startY = searchBox.y + FIELD_HEIGHT + 10;
        int endY = this.height - 70;
        int boxHeight = endY - startY;
        int contentWidth = FIELD_WIDTH - SCROLLBAR_WIDTH - 2;

        fill(poseStack, startX, startY, startX + FIELD_WIDTH, endY, 0xDD000000);
        fill(poseStack, startX, startY, startX + FIELD_WIDTH, startY + 1, 0xFF555555);
        fill(poseStack, startX, endY - 1, startX + FIELD_WIDTH, endY, 0xFF555555);

        if (filtered.isEmpty()) {
            String noResults = query.isEmpty() ? "Start typing to search..." : "No sounds found";
            drawCenteredString(poseStack, this.font, noResults,
                    startX + contentWidth / 2, startY + boxHeight / 2 - 4, 0x888888);
        } else {
            int maxVisible = boxHeight / SUGGESTION_HEIGHT;
            int maxScroll = Math.max(0, filtered.size() - maxVisible);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            String lastCategory = null;
            int renderY = startY;
            int visibleCount = 0;

            for (int i = scrollOffset; i < filtered.size() && visibleCount < maxVisible; i++) {
                SuggestionEntry entry = filtered.get(i);

                if (renderY + SUGGESTION_HEIGHT > endY) break;

                boolean isHovered = mouseX >= startX && mouseX <= startX + contentWidth &&
                        mouseY >= renderY && mouseY <= renderY + SUGGESTION_HEIGHT;

                if (i % 2 == 0) {
                    fill(poseStack, startX, renderY, startX + contentWidth, renderY + SUGGESTION_HEIGHT, 0x20FFFFFF);
                }
                if (isHovered) {
                    fill(poseStack, startX, renderY, startX + contentWidth, renderY + SUGGESTION_HEIGHT, 0x40FFFFFF);
                }

                if (!entry.category.equals(lastCategory)) {
                    fill(poseStack, startX, renderY, startX + 4, renderY + SUGGESTION_HEIGHT, getCategoryColor(entry.category));
                    lastCategory = entry.category;
                }

                String displayText = entry.soundId;
                int maxTextWidth = contentWidth - 50;
                if (this.font.width(displayText) > maxTextWidth) {
                    displayText = truncateText(displayText, maxTextWidth);
                }

                boolean isBlocked = blockedSounds.contains(entry.soundId);
                int textColor = isBlocked ? 0xFF5555 : 0xFFFFFF;
                this.font.draw(poseStack, displayText, startX + 10, renderY + 6, textColor);

                if (isBlocked) {
                    fill(poseStack, startX + contentWidth - 25, renderY + 5, startX + contentWidth - 5, renderY + SUGGESTION_HEIGHT - 5, 0xFF555555);
                    this.font.draw(poseStack, "✓", startX + contentWidth - 20, renderY + 6, 0xFFFFFF);
                }

                renderY += SUGGESTION_HEIGHT;
                visibleCount++;
            }

            if (filtered.size() > maxVisible) {
                int scrollbarX = startX + FIELD_WIDTH - SCROLLBAR_WIDTH;
                int scrollbarTrackHeight = boxHeight;
                int scrollbarThumbHeight = Math.max(30, (maxVisible * scrollbarTrackHeight) / filtered.size());
                int scrollbarThumbY = startY + (scrollOffset * (scrollbarTrackHeight - scrollbarThumbHeight)) /
                        Math.max(1, filtered.size() - maxVisible);

                fill(poseStack, scrollbarX, startY, scrollbarX + SCROLLBAR_WIDTH, endY, 0xFF222222);

                boolean isScrollbarHovered = mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                        mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight;

                int thumbColor = isDraggingScrollbar ? 0xFFCCCCCC : (isScrollbarHovered ? 0xFFBBBBBB : 0xFFAAAAAA);
                fill(poseStack, scrollbarX + 1, scrollbarThumbY, scrollbarX + SCROLLBAR_WIDTH - 1,
                        scrollbarThumbY + scrollbarThumbHeight, thumbColor);
            }
        }
    }

    private void renderSuggestionTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        String query = searchBox.getValue().toLowerCase().trim();
        List<SuggestionEntry> filtered = suggestions.stream()
                .filter(s -> {
                    if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                        return false;
                    }
                    return query.isEmpty() ||
                            s.soundId.toLowerCase().contains(query) ||
                            s.category.toLowerCase().contains(query);
                })
                .collect(Collectors.toList());

        int startX = this.width / 2 - FIELD_WIDTH / 2;
        int startY = searchBox.y + FIELD_HEIGHT + 10;
        int endY = this.height - 70;
        int boxHeight = endY - startY;
        int contentWidth = FIELD_WIDTH - SCROLLBAR_WIDTH - 2;

        if (filtered.isEmpty()) return;

        if (mouseX >= startX && mouseX <= startX + contentWidth &&
                mouseY >= startY && mouseY < endY) {

            int relativeY = mouseY - startY;
            int index = (relativeY / SUGGESTION_HEIGHT) + scrollOffset;

            if (index >= 0 && index < filtered.size()) {
                SuggestionEntry entry = filtered.get(index);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("§6" + entry.category));

                if (blockedSounds.contains(entry.soundId)) {
                    tooltip.add(Component.literal("§cClick to unblock"));
                } else {
                    tooltip.add(Component.literal("§aClick to block"));
                }

                renderComponentTooltip(poseStack, tooltip, mouseX, mouseY);
            }
        }
    }

    private int getCategoryColor(String category) {
        return switch (category) {
            case "Explosions" -> 0xFFFF5555;
            case "Gunfire" -> 0xFFFFAA00;
            case "Blocks" -> 0xFF55FF55;
            case "Movement" -> 0xFF5555FF;
            case "Entity Sounds" -> 0xFFFF55FF;
            case "Music" -> 0xFF55FFFF;
            default -> 0xFFAAAAAA;
        };
    }

    private String truncateText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;

        String ellipsis = "...";
        StringBuilder truncated = new StringBuilder();
        for (char c : text.toCharArray()) {
            truncated.append(c);
            if (this.font.width(truncated + ellipsis) >= maxWidth) {
                truncated.setLength(truncated.length() - 1);
                break;
            }
        }

        return truncated + ellipsis;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            String query = searchBox.getValue().toLowerCase().trim();
            List<SuggestionEntry> filtered = suggestions.stream()
                    .filter(s -> {
                        if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                            return false;
                        }
                        return query.isEmpty() ||
                                s.soundId.toLowerCase().contains(query) ||
                                s.category.toLowerCase().contains(query);
                    })
                    .collect(Collectors.toList());

            int startX = this.width / 2 - FIELD_WIDTH / 2;
            int startY = searchBox.y + FIELD_HEIGHT + 10;
            int endY = this.height - 70;
            int boxHeight = endY - startY;
            int contentWidth = FIELD_WIDTH - SCROLLBAR_WIDTH - 2;

            int maxVisible = boxHeight / SUGGESTION_HEIGHT;

            if (filtered.size() > maxVisible) {
                int scrollbarX = startX + FIELD_WIDTH - SCROLLBAR_WIDTH;
                int scrollbarTrackHeight = boxHeight;
                int scrollbarThumbHeight = Math.max(30, (maxVisible * scrollbarTrackHeight) / filtered.size());
                int scrollbarThumbY = startY + (scrollOffset * (scrollbarTrackHeight - scrollbarThumbHeight)) /
                        Math.max(1, filtered.size() - maxVisible);

                if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                        mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight) {
                    isDraggingScrollbar = true;
                    dragStartY = (int) mouseY;
                    dragStartOffset = scrollOffset;
                    return true;
                }
            }

            if (mouseX >= startX && mouseX <= startX + contentWidth &&
                    mouseY >= startY && mouseY < endY) {

                int relativeY = (int) mouseY - startY;
                int index = (relativeY / SUGGESTION_HEIGHT) + scrollOffset;

                if (index >= 0 && index < filtered.size()) {
                    SuggestionEntry entry = filtered.get(index);
                    toggleBlockedSound(entry.soundId);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            String query = searchBox.getValue().toLowerCase().trim();
            long filteredCount = suggestions.stream()
                    .filter(s -> {
                        if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                            return false;
                        }
                        return query.isEmpty() ||
                                s.soundId.toLowerCase().contains(query) ||
                                s.category.toLowerCase().contains(query);
                    })
                    .count();

            int startY = searchBox.y + FIELD_HEIGHT + 10;
            int endY = this.height - 70;
            int boxHeight = endY - startY;
            int maxVisible = boxHeight / SUGGESTION_HEIGHT;

            int scrollbarTrackHeight = boxHeight;
            int scrollbarThumbHeight = Math.max(30, (maxVisible * scrollbarTrackHeight) / (int) filteredCount);
            int availableTrackHeight = scrollbarTrackHeight - scrollbarThumbHeight;

            int deltaY = (int) mouseY - dragStartY;
            int maxScroll = Math.max(0, (int) filteredCount - maxVisible);

            if (availableTrackHeight > 0) {
                int deltaScroll = (deltaY * maxScroll) / availableTrackHeight;
                scrollOffset = Math.max(0, Math.min(dragStartOffset + deltaScroll, maxScroll));
            }

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void toggleBlockedSound(String soundId) {
        if (blockedSounds.contains(soundId)) {
            blockedSounds.remove(soundId);
        } else {
            blockedSounds.add(soundId);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int startX = this.width / 2 - FIELD_WIDTH / 2;
        int startY = searchBox.y + FIELD_HEIGHT + 10;
        int endY = this.height - 70;

        if (mouseX >= startX && mouseX <= startX + FIELD_WIDTH &&
                mouseY >= startY && mouseY < endY) {

            scrollOffset -= (int) delta;

            String query = searchBox.getValue().toLowerCase().trim();
            long filteredCount = suggestions.stream()
                    .filter(s -> {
                        if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) {
                            return false;
                        }
                        return query.isEmpty() ||
                                s.soundId.toLowerCase().contains(query) ||
                                s.category.toLowerCase().contains(query);
                    })
                    .count();

            int boxHeight = endY - startY;
            int maxVisible = boxHeight / SUGGESTION_HEIGHT;
            int maxScroll = Math.max(0, (int) filteredCount - maxVisible);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private record SuggestionEntry(String soundId, String category) {
    }
}