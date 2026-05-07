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
                    ROOT = new RootConfig();
                    MONSTER_CONFIGS = new HashMap<>();
                    ANIMAL_CONFIGS = new HashMap<>();
                }
            } catch (JsonParseException | MalformedJsonException e) {
                EZVCSurvival.LOGGER.warn("Malformed JSON in entities_voices.json, using defaults (no backup): {}", e.getMessage());
                ROOT = new RootConfig();
                MONSTER_CONFIGS = new HashMap<>();
                ANIMAL_CONFIGS = new HashMap<>();
                generateDefaults();
                save(path);
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warn("Error reading entities_voices.json, regenerating: {}", e.getMessage());
                ROOT = new RootConfig();
                MONSTER_CONFIGS = new HashMap<>();
                ANIMAL_CONFIGS = new HashMap<>();
                generateDefaults();
                save(path);
            }
        } else {
            ROOT = new RootConfig();
            generateDefaults();
            save(path);
        }

        boolean addedNew = ensureAllEntitiesPresent();
        if (addedNew) {
            save(path);
        }
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
            if (isMonsterCategory(category)) {
                MONSTER_CONFIGS.put(
                        Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString(),
                        EntityConfig.defaultFor(type)
                );
            } else if (isAnimalLikeCategory(category)) {
                ANIMAL_CONFIGS.put(
                        Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString(),
                        EntityConfig.defaultFor(type)
                );
            }
        }

        putIfPresent(MONSTER_CONFIGS, "minecraft:zombie", new EntityConfig(true, 1.0, 60.0, -20.0));
        putIfPresent(MONSTER_CONFIGS, "minecraft:skeleton", new EntityConfig(true, 1.0, 40.0, -15.0));
        putIfPresent(MONSTER_CONFIGS, "quiet_place:death_angel", new EntityConfig(true, 1.0, 50.0, -10.0));
        putIfPresent(ANIMAL_CONFIGS, "minecraft:cow", new EntityConfig(true, 1.0, 25.0, -18.0));
        putIfPresent(ANIMAL_CONFIGS, "minecraft:pig", new EntityConfig(true, 1.0, 15.0, -18.0));
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

    private static void putIfPresent(Map<String, EntityConfig> map, String id, EntityConfig config) {
        if (map.containsKey(id)) {
            map.put(id, config);
        }
    }

    private static void save(Path path) {
        if (MONSTER_CONFIGS == null || ANIMAL_CONFIGS == null) {
            EZVCSurvival.LOGGER.warn("Cannot save null configuration maps");
            return;
        }

        try {
            Files.createDirectories(path.getParent());

            if (ROOT == null) {
                ROOT = new RootConfig();
            }

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
                System.out.println("[EZVCSurvival] Successfully saved entities_voices.json with enabled=" + ROOT.enabled);
            }

        } catch (IOException e) {
            EZVCSurvival.LOGGER.error("Failed to save entities_voices.json: {}", e.getMessage());
        }
    }

    public static boolean isEnabled() {
        return ROOT == null || ROOT.enabled;
    }

    public static void setEnabled(boolean enabled) {
        if (ROOT == null) ROOT = new RootConfig();
        ROOT.enabled = enabled;
        persist();

        if (VoiceConfig.DEBUG.get()) {
            System.out.println("[EZVCSurvival] EntityVoiceConfig setEnabled called: " + enabled);
        }
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
            double baseSpeed = 1.0;
            double baseRange = 50.0;
            double baseThreshold = -20.0;

            if (type != null && type.getCategory() == MobCategory.MONSTER) {
                baseRange = 60.0;
            }
            return new EntityConfig(false, baseSpeed, baseRange, baseThreshold);
        }
    }

    public static final class RootConfig {
        public boolean enabled = true;
        public Map<String, EntityConfig> monsters;
        public Map<String, EntityConfig> animals;
    }

    public static EntityConfig getOrCreate(String entityId) {
        EntityConfig config = get(entityId);

        if (config != null) {
            return config;
        }

        config = new EntityConfig(
                true,
                1.0,
                60.0,
                -20.0
        );

        set(entityId, config);
        persist();

        System.out.println("[EZVCSurvival] Created default voice config for: " + entityId);

        return config;
    }

}