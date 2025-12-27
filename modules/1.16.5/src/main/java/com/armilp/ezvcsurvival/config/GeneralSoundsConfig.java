package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.armilp.ezvcsurvival.data.SoundGroupData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class GeneralSoundsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_TYPE = new TypeToken<Root>() {
    }.getType();

    public static Root ROOT = new Root();
    private static boolean isInitialized = false;

    private GeneralSoundsConfig() {
    }

    public static void init() {
        if (!isInitialized) {
            loadOrCreate();
            isInitialized = true;
        } else {
            reloadFromDisk();
        }
    }

    private static void reloadFromDisk() {
        Path path = getPath();
        if (Files.exists(path)) {
            try {
                BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                try {
                    Root loaded = GSON.fromJson(reader, ROOT_TYPE);
                    if (loaded != null) {
                        ROOT = loaded;
                        if (ROOT.mobs == null) ROOT.mobs = new HashMap<String, Reaction>();
                        if (ROOT.sounds == null) ROOT.sounds = new HashMap<String, SoundEntry>();
                    }
                } finally {
                    reader.close();
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.warning("Error reloading generalsounds.json: {}");
            }
        }
    }

    public static Map<String, Reaction> getMobReactions() {
        if (ROOT == null) ROOT = new Root();
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<String, Reaction>();
        return ROOT.mobs;
    }

    public static void setMobReaction(String entityId, boolean enabled, double speed, double range) {
        if (ROOT == null) ROOT = new Root();
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<String, Reaction>();

        Reaction existing = ROOT.mobs.get(entityId);
        if (existing != null) {
            existing.enabled = enabled;
            existing.speed = speed;
            existing.range = range;
        } else {
            ROOT.mobs.put(entityId, new Reaction(enabled, speed, range));
        }
    }

    public static boolean canEntityReactToSound(String entityId, String soundId) {
        if (ROOT == null || ROOT.mobs == null) return true;

        Reaction reaction = ROOT.mobs.get(entityId);
        if (reaction == null || reaction.blocked_sounds == null || reaction.blocked_sounds.isEmpty()) {
            return true;
        }

        String normalizedSoundId = soundId.toLowerCase();

        for (String blocked : reaction.blocked_sounds) {
            if (blocked == null || blocked.trim().isEmpty()) continue;

            String normalizedBlocked = blocked.toLowerCase().trim();

            if (normalizedSoundId.equals(normalizedBlocked)) {
                return false;
            }

            if (normalizedBlocked.contains("*")) {
                if (matchesWildcard(normalizedSoundId, normalizedBlocked)) {
                    return false;
                }
            } else {
                if (normalizedSoundId.startsWith(normalizedBlocked)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean matchesWildcard(String text, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");

        try {
            return text.matches(regex);
        } catch (Exception e) {
            return text.startsWith(pattern.replace("*", ""));
        }
    }

    public static Map<String, SoundEntry> getSounds() {
        if (ROOT == null) ROOT = new Root();
        if (ROOT.sounds == null) ROOT.sounds = new HashMap<String, SoundEntry>();
        return ROOT.sounds;
    }

    public static void setSoundEntry(String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier) {
        setSoundEntry(soundId, enabled, speedMultiplier, rangeMultiplier, false);
    }

    public static void setSoundEntry(String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier, boolean isPriority) {
        if (ROOT == null) ROOT = new Root();
        if (ROOT.sounds == null) ROOT.sounds = new HashMap<String, SoundEntry>();

        SoundEntry existing = ROOT.sounds.get(soundId);
        if (existing != null) {
            existing.enabled = enabled;
            existing.speed_multiplier = speedMultiplier;
            existing.range_multiplier = rangeMultiplier;
            existing.is_priority = isPriority;
        } else {
            SoundEntry newEntry = new SoundEntry(enabled, speedMultiplier, rangeMultiplier, isPriority);
            ROOT.sounds.put(soundId, newEntry);
        }
    }

    public static void processPrioritySounds(List<SoundGroupData> priorityGroups) {
        Map<String, SoundEntry> sounds = getSounds();
        if (sounds == null) return;

        Iterator<SoundGroupData> iterator = priorityGroups.iterator();
        while (iterator.hasNext()) {
            SoundGroupData group = iterator.next();
            if (group.groupName.startsWith("auto_priority_")) {
                iterator.remove();
            }
        }

        for (Map.Entry<String, SoundEntry> entry : sounds.entrySet()) {
            String soundId = entry.getKey();
            SoundEntry soundEntry = entry.getValue();

            if (soundEntry.enabled && soundEntry.is_priority) {
                String groupName = "auto_priority_" + soundId.replace(':', '_').replace('.', '_');

                SoundGroupData priorityGroup = createPriorityGroup(groupName, soundId, soundEntry);
                if (priorityGroup != null) {
                    priorityGroups.add(priorityGroup);
                    EZVCSurvival.LOGGER.fine("Added active priority group: {} for sound: {}");
                }
            }
        }
    }

    private static SoundGroupData createPriorityGroup(String groupName, String soundId, SoundEntry config) {
        try {
            double prioritySpeedMultiplier = config.speed_multiplier * 1.5;
            double priorityRangeMultiplier = config.range_multiplier * 1.5;

            return new SoundGroupData(groupName, Collections.singletonList(soundId), prioritySpeedMultiplier, priorityRangeMultiplier);
        } catch (Exception e) {
            EZVCSurvival.LOGGER.warning("Could not create priority sound group for {}: {}");
            return null;
        }
    }

    public static void enableAllPrioritySounds(boolean enabled) {
        Map<String, SoundEntry> sounds = getSounds();
        boolean changed = false;

        for (Map.Entry<String, SoundEntry> entry : sounds.entrySet()) {
            SoundEntry sound = entry.getValue();
            if (sound.is_priority) {
                sound.enabled = enabled;
                changed = true;
                EZVCSurvival.LOGGER.fine("Set priority sound '{}' enabled to: {}");
            }
        }

        if (changed) {
            persist();
            EZVCSurvival.LOGGER.info("Updated all priority sounds to enabled={}");
        }
    }

    public static boolean isEnabled() {
        return ROOT == null || ROOT.enabled;
    }

    public static void setEnabled(boolean enabled) {
        if (ROOT == null) ROOT = new Root();
        ROOT.enabled = enabled;
        persist();
    }

    public static void persist() {
        save(getPath());
    }

    private static Path getPath() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("ezvcsurvival");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {
        }
        return configDir.resolve("generalsounds.json");
    }

    private static void loadOrCreate() {
        Path path = getPath();
        if (Files.exists(path)) {
            try {
                BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                try {
                    Root loaded = GSON.fromJson(reader, ROOT_TYPE);
                    if (loaded != null) {
                        ROOT = loaded;
                        if (ROOT.mobs == null) ROOT.mobs = new HashMap<String, Reaction>();
                        if (ROOT.sounds == null) ROOT.sounds = new HashMap<String, SoundEntry>();
                    } else {
                        ROOT = new Root();
                        generateDefaults();
                        save(path);
                    }
                } finally {
                    reader.close();
                }
            } catch (JsonParseException e) {
                EZVCSurvival.LOGGER.warning("Malformed JSON in generalsounds.json, using defaults (no backup): {}");
                ROOT = new Root();
                generateDefaults();
                save(path);
            } catch (MalformedJsonException e) {
                EZVCSurvival.LOGGER.warning("Malformed JSON in generalsounds.json, using defaults (no backup): {}");
                ROOT = new Root();
                generateDefaults();
                save(path);
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warning("Error reading generalsounds.json, regenerating: {}");
                ROOT = new Root();
                generateDefaults();
                save(path);
            }
        } else {
            ROOT = new Root();
            generateDefaults();
            save(path);
        }

        boolean addedNew = ensureNewENTITY_TYPESOnly();
        if (addedNew) {
            save(path);
        }
    }

    private static boolean ensureNewENTITY_TYPESOnly() {
        boolean added = false;

        if (ROOT.mobs == null) {
            ROOT.mobs = new HashMap<String, Reaction>();
        }

        for (EntityType<?> type : ForgeRegistries.ENTITIES) {
            EntityClassification category = type.getCategory();
            if (category == EntityClassification.MISC) continue;
            String id = Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString();
            if (!ROOT.mobs.containsKey(id)) {
                ROOT.mobs.put(id, Reaction.defaultFor(type));
                added = true;
            }
        }

        if (ROOT.sounds == null) {
            ROOT.sounds = new HashMap<String, SoundEntry>();
        }

        for (Object sound : ForgeRegistries.SOUND_EVENTS) {
            String id = Objects.requireNonNull(ForgeRegistries.SOUND_EVENTS.getKey((net.minecraft.util.SoundEvent) sound)).toString();

            if (!ROOT.sounds.containsKey(id)) {
                boolean shouldEnable = shouldEnableByDefault(id);
                boolean shouldBePriority = shouldBePriorityByDefault(id);
                ROOT.sounds.put(id, new SoundEntry(shouldEnable, 1.0, 1.0, shouldBePriority));
                added = true;
            }
        }

        return added;
    }

    private static boolean shouldEnableByDefault(String soundId) {
        if (soundId.startsWith("pointblank:") || soundId.contains("superbwarfare:")) {
            return false;
        }

        return soundId.contains("place") || soundId.contains("break") ||
                soundId.contains("explode") || soundId.contains("explosion");
    }

    private static boolean shouldBePriorityByDefault(String soundId) {
        if (soundId.startsWith("pointblank:") || soundId.contains("superbwarfare:")) {
            return false;
        }

        return soundId.contains("explode") || soundId.contains("explosion");
    }

    private static void generateDefaults() {
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<String, Reaction>();
        if (ROOT.sounds == null) ROOT.sounds = new HashMap<String, SoundEntry>();

        ROOT.mobs.clear();

        for (EntityType<?> type : ForgeRegistries.ENTITIES) {
            EntityClassification category = type.getCategory();
            if (category == EntityClassification.MISC) continue;
            ROOT.mobs.put(
                    Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString(),
                    Reaction.defaultFor(type)
            );
        }

        putIfPresent(ROOT.mobs, "minecraft:zombie", new Reaction(true, 1.7, 60.0));
        putIfPresent(ROOT.mobs, "minecraft:skeleton", new Reaction(true, 1.2, 40.0));
        putIfPresent(ROOT.mobs, "quiet_place:death_angel", new Reaction(true, 1.2, 50.0));
        putIfPresent(ROOT.mobs, "minecraft:cow", new Reaction(true, 1.5, 25.0));
        putIfPresent(ROOT.mobs, "minecraft:pig", new Reaction(true, 1.2, 15.0));
    }

    private static void putIfPresent(Map<String, Reaction> map, String id, Reaction config) {
        if (map.containsKey(id)) {
            map.put(id, config);
        }
    }

    private static void save(Path path) {
        if (ROOT == null) {
            EZVCSurvival.LOGGER.warning("Cannot save null configuration");
            return;
        }

        try {
            Files.createDirectories(path.getParent());

            BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            try {
                GSON.toJson(ROOT, ROOT_TYPE, writer);
                writer.flush();
            } finally {
                writer.close();
            }

        } catch (IOException e) {
            EZVCSurvival.LOGGER.severe("Failed to save generalsounds.json: {}");
        }
    }

    public static final class Root {
        public boolean enabled = true;
        public Map<String, Reaction> mobs;
        public Map<String, SoundEntry> sounds;
    }

    public static final class Reaction {
        public boolean enabled;
        public double speed;
        public double range;
        public List<String> blocked_sounds;

        public Reaction(boolean enabled, double speed, double range) {
            this(enabled, speed, range, new ArrayList<String>());
        }

        public Reaction(boolean enabled, double speed, double range, List<String> blockedSounds) {
            this.enabled = enabled;
            this.speed = speed;
            this.range = range;
            this.blocked_sounds = blockedSounds != null ? new ArrayList<String>(blockedSounds) : new ArrayList<String>();
        }

        public static Reaction defaultFor(EntityType<?> type) {
            double baseSpeed = 1.0;
            double baseRange = 50.0;

            if (type.getCategory() == EntityClassification.MONSTER) {
                baseRange = 60.0;
            }
            return new Reaction(false, baseSpeed, baseRange);
        }
    }

    public static final class SoundEntry {
        public boolean enabled;
        public double speed_multiplier;
        public double range_multiplier;
        public boolean is_priority;

        public SoundEntry(boolean enabled, double speedMultiplier, double rangeMultiplier) {
            this(enabled, speedMultiplier, rangeMultiplier, false);
        }

        public SoundEntry(boolean enabled, double speedMultiplier, double rangeMultiplier, boolean isPriority) {
            this.enabled = enabled;
            this.speed_multiplier = speedMultiplier;
            this.range_multiplier = rangeMultiplier;
            this.is_priority = isPriority;
        }
    }
}