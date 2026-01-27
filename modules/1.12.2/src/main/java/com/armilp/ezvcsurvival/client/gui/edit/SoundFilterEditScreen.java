package com.armilp.ezvcsurvival.client.gui.edit;

import com.armilp.ezvcsurvival.config.GeneralSoundsConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class SoundFilterEditScreen extends GuiScreen {
    private final GuiScreen parent;
    private final String entityId;
    private final String elementName;
    private String screenTitle;

    private GuiTextField searchBox;
    private GuiButton saveButton;
    private GuiButton backButton;
    private GuiButton clearButton;
    private GuiButton toggleFilterButton;

    private List<String> blockedSounds;
    private List<SuggestionEntry> suggestions;
    private int scrollOffset = 0;
    private boolean isDraggingScrollbar = false;
    private int dragStartY = 0;
    private int dragStartOffset = 0;
    private boolean showOnlyBlocked = false;

    private static final int MAX_VISIBLE_SUGGESTIONS = 15;
    private static final int FIELD_WIDTH = 500;
    private static final int FIELD_HEIGHT = 20;
    private static final int SUGGESTION_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 10;

    public SoundFilterEditScreen(GuiScreen parent, String entityId, String elementName) {
        this.parent = parent;
        this.entityId = entityId;
        this.elementName = elementName;
        this.screenTitle = "Edit Sound Filter: " + elementName;
        loadCurrentFilters();
        loadSuggestions();
    }

    private void loadCurrentFilters() {
        GeneralSoundsConfig.Reaction reaction = GeneralSoundsConfig.getMobReactions().get(entityId);
        if (reaction != null && reaction.blocked_sounds != null) {
            this.blockedSounds = new ArrayList<String>(reaction.blocked_sounds);
        } else {
            this.blockedSounds = new ArrayList<String>();
        }
    }

    private void loadSuggestions() {
        suggestions = new ArrayList<SuggestionEntry>();

        try {
            @SuppressWarnings("unchecked")
            Set<ResourceLocation> keys = (Set<ResourceLocation>) SoundEvent.REGISTRY.getKeys();
            for (ResourceLocation key : keys) {
                String soundId = key.toString();
                String category = categorizeSound(soundId);
                suggestions.add(new SuggestionEntry(soundId, category));
            }
        } catch (Throwable t) {
            for (Object obj : SoundEvent.REGISTRY) {
                SoundEvent sound = (SoundEvent) obj;
                ResourceLocation key = SoundEvent.REGISTRY.getNameForObject(sound);
                if (key != null) {
                    String soundId = key.toString();
                    String category = categorizeSound(soundId);
                    suggestions.add(new SuggestionEntry(soundId, category));
                }
            }
        }

        Collections.sort(suggestions, new Comparator<SuggestionEntry>() {
            @Override
            public int compare(SuggestionEntry a, SuggestionEntry b) {
                int catCompare = a.category.compareTo(b.category);
                return catCompare != 0 ? catCompare : a.soundId.compareTo(b.soundId);
            }
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
    public void initGui() {
        super.initGui();

        int centerX = this.width / 2;
        int startY = 40;

        searchBox = new GuiTextField(0, this.fontRenderer, centerX - FIELD_WIDTH / 2, startY, FIELD_WIDTH - 110, FIELD_HEIGHT);
        searchBox.setText("Search Sounds");
        searchBox.setMaxStringLength(100);
        searchBox.setFocused(true);

        toggleFilterButton = new GuiButton(1, centerX + FIELD_WIDTH / 2 - 100, startY, 100, FIELD_HEIGHT, "Show Blocked");
        this.buttonList.add(toggleFilterButton);

        int buttonY = this.height - 30;

        clearButton = new GuiButton(2, centerX - 210, buttonY, 100, 20, "Clear All");
        this.buttonList.add(clearButton);

        saveButton = new GuiButton(3, centerX - 100, buttonY, 100, 20, "Save");
        this.buttonList.add(saveButton);

        backButton = new GuiButton(4, centerX + 10, buttonY, 100, 20, "Cancel");
        this.buttonList.add(backButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null) return;
        if (!button.enabled) return;

        switch (button.id) {
            case 1:
                toggleShowBlocked();
                break;
            case 2:
                clearFilters();
                break;
            case 3:
                saveFilters();
                break;
            case 4:
                Minecraft.getMinecraft().displayGuiScreen(parent);
                break;
        }
    }

    private void onSearchChanged(String query) {
        scrollOffset = 0;
    }

    private void toggleShowBlocked() {
        showOnlyBlocked = !showOnlyBlocked;
        scrollOffset = 0;
        toggleFilterButton.displayString = showOnlyBlocked ? "Show All" : "Show Blocked";
    }

    private void clearFilters() {
        blockedSounds.clear();
    }

    private void saveFilters() {
        GeneralSoundsConfig.Reaction reaction = GeneralSoundsConfig.getMobReactions().get(entityId);
        if (reaction != null) {
            reaction.blocked_sounds = new ArrayList<String>(blockedSounds);
            GeneralSoundsConfig.persist();
        }
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        drawCenteredString(this.fontRenderer, this.screenTitle, this.width / 2, 15, 0xFFFFFF);

        this.fontRenderer.drawString("Search and click to block/unblock sounds:", this.width / 2 - FIELD_WIDTH / 2, 25, 0xFFFFFF);

        renderSuggestions(mouseX, mouseY);

        drawCenteredString(this.fontRenderer, "Blocked: " + blockedSounds.size() + " sounds", this.width / 2, this.height - 50, 0xAAAAAA);

        searchBox.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);

        renderSuggestionTooltip(mouseX, mouseY);

        int dWheel = net.minecraft.client.gui.GuiScreen.isCtrlKeyDown() ? 0 : 0; // placeholder si se necesita
        int wheel = net.minecraft.client.Minecraft.getMinecraft().mouseHelper == null ? 0 : 0;
        FrameTimer d = Minecraft.getMinecraft().getFrameTimer(); // no usado; scroll se maneja por handleMouseInput altern.
        int wheelDelta = Mouse.getDWheel();
        if (wheelDelta != 0) {
            int step = wheelDelta > 0 ? 1 : -1;
            handleMouseWheel(-step, mouseX, mouseY);
        }
    }

    private void handleMouseWheel(int steps, int mouseX, int mouseY) {
        int startX = this.width / 2 - FIELD_WIDTH / 2;
        int startY = searchBox.y + FIELD_HEIGHT + 10;
        int endY = this.height - 70;

        if (mouseX >= startX && mouseX <= startX + FIELD_WIDTH &&
                mouseY >= startY && mouseY < endY) {

            scrollOffset -= steps;

            String query = searchBox.getText().toLowerCase().trim();
            int filteredCount = 0;
            for (SuggestionEntry s : suggestions) {
                if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) continue;
                if (query.isEmpty() || s.soundId.toLowerCase().contains(query) || s.category.toLowerCase().contains(query)) {
                    filteredCount++;
                }
            }

            int boxHeight = endY - startY;
            int maxVisible = boxHeight / SUGGESTION_HEIGHT;
            int maxScroll = Math.max(0, filteredCount - maxVisible);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }
    }

    private void renderSuggestions(int mouseX, int mouseY) {
        String query = searchBox.getText().toLowerCase().trim();

        List<SuggestionEntry> filtered = new ArrayList<SuggestionEntry>();
        for (SuggestionEntry s : suggestions) {
            if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) continue;
            if (query.isEmpty() || s.soundId.toLowerCase().contains(query) || s.category.toLowerCase().contains(query)) {
                filtered.add(s);
            }
        }

        int startX = this.width / 2 - FIELD_WIDTH / 2;
        int startY = searchBox.y + FIELD_HEIGHT + 10;
        int endY = this.height - 70;
        int boxHeight = endY - startY;
        int contentWidth = FIELD_WIDTH - SCROLLBAR_WIDTH - 2;

        drawRect(startX, startY, startX + FIELD_WIDTH, endY, 0xDD000000);
        drawRect(startX, startY, startX + FIELD_WIDTH, startY + 1, 0xFF555555);
        drawRect(startX, endY - 1, startX + FIELD_WIDTH, endY, 0xFF555555);

        if (filtered.isEmpty()) {
            String noResults = query.isEmpty() ? "Start typing to search..." : "No sounds found";
            drawCenteredString(this.fontRenderer, noResults, startX + contentWidth / 2, startY + boxHeight / 2 - 4, 0x888888);
        } else {
            int maxVisible = boxHeight / SUGGESTION_HEIGHT;
            int maxScroll = Math.max(0, filtered.size() - maxVisible);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            String lastCategory = null;
            int renderY = startX * 0;
            renderY = startY;
            int visibleCount = 0;

            for (int i = scrollOffset; i < filtered.size() && visibleCount < maxVisible; i++) {
                SuggestionEntry entry = filtered.get(i);

                if (renderY + SUGGESTION_HEIGHT > endY) break;

                boolean isHovered = mouseX >= startX && mouseX <= startX + contentWidth &&
                        mouseY >= renderY && mouseY <= renderY + SUGGESTION_HEIGHT;

                if (i % 2 == 0) {
                    drawRect(startX, renderY, startX + contentWidth, renderY + SUGGESTION_HEIGHT, 0x20FFFFFF);
                }
                if (isHovered) {
                    drawRect(startX, renderY, startX + contentWidth, renderY + SUGGESTION_HEIGHT, 0x40FFFFFF);
                }

                if (!entry.category.equals(lastCategory)) {
                    drawRect(startX, renderY, startX + 4, renderY + SUGGESTION_HEIGHT, getCategoryColor(entry.category));
                    lastCategory = entry.category;
                }

                String displayText = entry.soundId;
                int maxTextWidth = contentWidth - 50;
                if (this.fontRenderer.getStringWidth(displayText) > maxTextWidth) {
                    displayText = truncateText(displayText, maxTextWidth);
                }

                boolean isBlocked = blockedSounds.contains(entry.soundId);
                int textColor = isBlocked ? 0xFF5555 : 0xFFFFFF;
                this.fontRenderer.drawString(displayText, startX + 10, renderY + 6, textColor);

                if (isBlocked) {
                    drawRect(startX + contentWidth - 25, renderY + 5, startX + contentWidth - 5, renderY + SUGGESTION_HEIGHT - 5, 0xFF555555);
                    this.fontRenderer.drawString("✓", startX + contentWidth - 20, renderY + 6, 0xFFFFFF);
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

                drawRect(scrollbarX, startY, scrollbarX + SCROLLBAR_WIDTH, endY, 0xFF222222);

                boolean isScrollbarHovered = mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH &&
                        mouseY >= scrollbarThumbY && mouseY <= scrollbarThumbY + scrollbarThumbHeight;

                int thumbColor = isDraggingScrollbar ? 0xFFCCCCCC : (isScrollbarHovered ? 0xFFBBBBBB : 0xFFAAAAAA);
                drawRect(scrollbarX + 1, scrollbarThumbY, scrollbarX + SCROLLBAR_WIDTH - 1,
                        scrollbarThumbY + scrollbarThumbHeight, thumbColor);
            }
        }
    }

    private void renderSuggestionTooltip(int mouseX, int mouseY) {
        String query = searchBox.getText().toLowerCase().trim();
        List<SuggestionEntry> filtered = new ArrayList<SuggestionEntry>();
        for (SuggestionEntry s : suggestions) {
            if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) continue;
            if (query.isEmpty() || s.soundId.toLowerCase().contains(query) || s.category.toLowerCase().contains(query)) {
                filtered.add(s);
            }
        }

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
                List<String> tooltip = new ArrayList<String>();
                tooltip.add(entry.category);
                if (blockedSounds.contains(entry.soundId)) {
                    tooltip.add("Click to unblock");
                } else {
                    tooltip.add("Click to block");
                }
                drawHoveringText(tooltip, mouseX, mouseY);
            }
        }
    }

    private int getCategoryColor(String category) {
        if (category.equals("Explosions")) return 0xFFFF5555;
        if (category.equals("Gunfire")) return 0xFFFFAA00;
        if (category.equals("Blocks")) return 0xFF55FF55;
        if (category.equals("Movement")) return 0xFF5555FF;
        if (category.equals("Entity Sounds")) return 0xFFFF55FF;
        if (category.equals("Music")) return 0xFF55FFFF;
        return 0xFFAAAAAA;
    }

    private String truncateText(String text, int maxWidth) {
        if (this.fontRenderer.getStringWidth(text) <= maxWidth) return text;

        String ellipsis = "...";
        StringBuilder truncated = new StringBuilder();
        for (char c : text.toCharArray()) {
            truncated.append(c);
            if (this.fontRenderer.getStringWidth(truncated.toString() + ellipsis) >= maxWidth) {
                truncated.setLength(truncated.length() - 1);
                break;
            }
        }

        return truncated.toString() + ellipsis;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // permitir que el textbox capture clicks
        searchBox.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            String query = searchBox.getText().toLowerCase().trim();
            List<SuggestionEntry> filtered = new ArrayList<SuggestionEntry>();
            for (SuggestionEntry s : suggestions) {
                if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) continue;
                if (query.isEmpty() || s.soundId.toLowerCase().contains(query) || s.category.toLowerCase().contains(query)) {
                    filtered.add(s);
                }
            }

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
                    dragStartY = mouseY;
                    dragStartOffset = scrollOffset;
                    return;
                }
            }

            if (mouseX >= startX && mouseX <= startX + contentWidth &&
                    mouseY >= startY && mouseY < endY) {

                int relativeY = mouseY - startY;
                int index = (relativeY / SUGGESTION_HEIGHT) + scrollOffset;

                if (index >= 0 && index < filtered.size()) {
                    SuggestionEntry entry = filtered.get(index);
                    toggleBlockedSound(entry.soundId);
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDraggingScrollbar) {
            String query = searchBox.getText().toLowerCase().trim();
            int filteredCount = 0;
            for (SuggestionEntry s : suggestions) {
                if (showOnlyBlocked && !blockedSounds.contains(s.soundId)) continue;
                if (query.isEmpty() || s.soundId.toLowerCase().contains(query) || s.category.toLowerCase().contains(query)) {
                    filteredCount++;
                }
            }

            int startY = searchBox.y + FIELD_HEIGHT + 10;
            int endY = this.height - 70;
            int boxHeight = endY - startY;
            int maxVisible = boxHeight / SUGGESTION_HEIGHT;

            int scrollbarTrackHeight = boxHeight;
            int scrollbarThumbHeight = Math.max(30, (maxVisible * scrollbarTrackHeight) / Math.max(1, filteredCount));
            int availableTrackHeight = scrollbarTrackHeight - scrollbarThumbHeight;

            int deltaY = mouseY - dragStartY;
            int maxScroll = Math.max(0, filteredCount - maxVisible);

            if (availableTrackHeight > 0) {
                int deltaScroll = (deltaY * maxScroll) / availableTrackHeight;
                scrollOffset = Math.max(0, Math.min(dragStartOffset + deltaScroll, maxScroll));
            }

            return;
        }

        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    private void toggleBlockedSound(String soundId) {
        if (blockedSounds.contains(soundId)) {
            blockedSounds.remove(soundId);
        } else {
            blockedSounds.add(soundId);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        // La rueda del ratón también la capturamos en drawScreen mediante Mouse.getDWheel(),
        // pero se podría optar por usar aquí Mouse.getDWheel() también.
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchBox.textboxKeyTyped(typedChar, keyCode)) {
            onSearchChanged(this.searchBox.getText());
            return;
        }

        if (keyCode == 1) { // ESC
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static class SuggestionEntry {
        public final String soundId;
        public final String category;

        public SuggestionEntry(String soundId, String category) {
            this.soundId = soundId;
            this.category = category;
        }
    }
}