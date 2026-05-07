package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.fml.loading.FMLPaths;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EntityVoiceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_TYPE = new TypeToken<RootConfig>() {
    }.getType();

    private static Map<String, EntityConfig> MONSTER_CONFIGS = new HashMap<>();
    private static Map<String, EntityConfig> ANIMAL_CONFIGS = new HashMap<>();
    public static RootConfig ROOT = new RootConfig();
    private static boolean isInitialized = false;

    private EntityVoiceConfig() {
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
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                RootConfig loaded = GSON.fromJson(reader, ROOT_TYPE);
                if (loaded != null) {
                    ROOT = loaded;
                    MONSTER_CONFIGS = loaded.monsters != null ? new HashMap<>(loaded.monsters) : new HashMap<>();
                    ANIMAL_CONFIGS = loaded.animals != null ? new HashMap<>(loaded.animals) : new HashMap<>();
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.warn("Error reloading entities_voices.json: {}", e.getMessage());
            }
        }
    }

    public static Set<String> getAllEntityIds() {
        java.util.HashSet<String> all = new java.util.HashSet<>();
        all.addAll(MONSTER_CONFIGS.keySet());
        all.addAll(ANIMAL_CONFIGS.keySet());
        return all;
    }

    public static EntityConfig getMonster(String entityId) {
        return MONSTER_CONFIGS.get(entityId);
    }

    public static EntityConfig getAnimal(String entityId) {
        return ANIMAL_CONFIGS.get(entityId);
    }

    public static EntityConfig get(String entityId) {
        EntityConfig ec = MONSTER_CONFIGS.get(entityId);
        if (ec == null) ec = ANIMAL_CONFIGS.get(entityId);
        return ec;
    }

    public static void set(String entityId, EntityConfig value) {
        if (MONSTER_CONFIGS.containsKey(entityId))
            MONSTER_CONFIGS.put(entityId, value);
        else
            ANIMAL_CONFIGS.put(entityId, value);
    }

    public static void persist() {
        save(getConfigPath());
    }

    private static Path getConfigPath() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("ezvcsurvival");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {
        }
        return configDir.resolve("entities_voices.json");
    }

    private static void loadOrCreate() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                RootConfig loaded = GSON.fromJson(reader, ROOT_TYPE);
                if (loaded != null) {
                    ROOT = loaded;
                    MONSTER_CONFIGS = loaded.monsters != null ? new HashMap<>(loaded.monsters) : new HashMap<>();
                    ANIMAL_CONFIGS = loaded.animals != null ? new HashMap<>(loaded.animals) : new HashMap<>();
                } else {
                    resetToDefaults();
                    generateDefaults();
                    save(path);
                }
            } catch (JsonParseException | MalformedJsonException e) {
                EZVCSurvival.LOGGER.warn("Malformed JSON in entities_voices.json, regenerating: {}", e.getMessage());
                resetToDefaults();
                generateDefaults();
                save(path);
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warn("Error reading entities_voices.json, regenerating: {}", e.getMessage());
                resetToDefaults();
                generateDefaults();
                save(path);
            }
        } else {
            resetToDefaults();
            generateDefaults();
            save(path);
        }

        boolean addedNew = ensureAllEntitiesPresent();
        if (addedNew) save(path);
    }

    private static void resetToDefaults() {
        ROOT = new RootConfig();
        MONSTER_CONFIGS = new HashMap<>();
        ANIMAL_CONFIGS = new HashMap<>();
    }

    private static boolean ensureAllEntitiesPresent() {
        boolean added = false;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;
            String id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
            if (isMonsterCategory(category)) {
                if (!MONSTER_CONFIGS.containsKey(id)) {
                    MONSTER_CONFIGS.put(id, EntityConfig.defaultFor(type));
                    added = true;
                }
            } else if (isAnimalLikeCategory(category)) {
                if (!ANIMAL_CONFIGS.containsKey(id)) {
                    ANIMAL_CONFIGS.put(id, EntityConfig.defaultFor(type));
                    added = true;
                }
            }
        }
        return added;
    }

    private static void generateDefaults() {
        MONSTER_CONFIGS.clear();
        ANIMAL_CONFIGS.clear();

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory category = type.getCategory();
            if (category == MobCategory.MISC) continue;
            String id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
            if (isMonsterCategory(category)) {
                MONSTER_CONFIGS.put(id, EntityConfig.defaultFor(type));
            } else if (isAnimalLikeCategory(category)) {
                ANIMAL_CONFIGS.put(id, EntityConfig.defaultFor(type));
            }
        }

        MONSTER_CONFIGS.put("minecraft:zombie", new EntityConfig(true, 1.0, 60.0, -20.0));
        MONSTER_CONFIGS.put("minecraft:skeleton", new EntityConfig(true, 1.0, 40.0, -15.0));
        MONSTER_CONFIGS.put("quiet_place:death_angel", new EntityConfig(true, 1.0, 50.0, -10.0));
        ANIMAL_CONFIGS.put("minecraft:cow", new EntityConfig(true, 1.0, 25.0, -18.0));
        ANIMAL_CONFIGS.put("minecraft:pig", new EntityConfig(true, 1.0, 15.0, -18.0));
    }

    private static boolean isMonsterCategory(MobCategory category) {
        return category == MobCategory.MONSTER;
    }

    private static boolean isAnimalLikeCategory(MobCategory category) {
        return category == MobCategory.CREATURE
                || category == MobCategory.AMBIENT
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.UNDERGROUND_WATER_CREATURE
                || category.name().equalsIgnoreCase("AXOLOTLS");
    }

    private static void save(Path path) {
        if (MONSTER_CONFIGS == null || ANIMAL_CONFIGS == null) {
            EZVCSurvival.LOGGER.warn("Cannot save null configuration maps");
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            if (ROOT == null) ROOT = new RootConfig();
            ROOT.monsters = MONSTER_CONFIGS;
            ROOT.animals = ANIMAL_CONFIGS;

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(ROOT, ROOT_TYPE, writer);
                writer.flush();
            }
            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Saved entities_voices.json (enabled=" + ROOT.enabled + ")");
            }
        } catch (IOException e) {
            EZVCSurvival.LOGGER.error("Failed to save entities_voices.json: {}", e.getMessage());
        }
    }

    public static boolean isEnabled() {
        return ROOT == null || ROOT.enabled;
    }

    public static void setEnabled(boolean value) {
        if (ROOT == null) ROOT = new RootConfig();
        ROOT.enabled = value;
        persist();
        if (VoiceConfig.DEBUG.get())
            System.out.println("[EZVCSurvival] EntityVoiceConfig setEnabled: " + value);
    }

    public static final class EntityConfig {
        public boolean enabled;
        public double speed;
        public double range;
        public double threshold;

        public EntityConfig(boolean enabled, double speed, double range, double threshold) {
            this.enabled = enabled;
            this.speed = speed;
            this.range = range;
            this.threshold = threshold;
        }

        public static EntityConfig defaultFor(EntityType<?> type) {
            boolean isMonster = type != null && type.getCategory() == MobCategory.MONSTER;
            double baseRange = isMonster ? 60.0 : 50.0;
            return new EntityConfig(isMonster, 1.0, baseRange, -20.0);
        }
    }

    public static final class RootConfig {
        public boolean enabled = true;
        public Map<String, EntityConfig> monsters;
        public Map<String, EntityConfig> animals;
    }
}