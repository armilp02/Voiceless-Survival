package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minecraft.entity.EnumCreatureType;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class EntityVoiceConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_TYPE = new TypeToken<RootConfig>() {
    }.getType();

    private static Map<String, EntityConfig> MONSTER_CONFIGS = new HashMap<String, EntityConfig>();
    private static Map<String, EntityConfig> ANIMAL_CONFIGS = new HashMap<String, EntityConfig>();
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
            try {
                BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                try {
                    RootConfig loaded = GSON.fromJson(reader, ROOT_TYPE);
                    if (loaded != null) {
                        ROOT = loaded;
                        MONSTER_CONFIGS = loaded.monsters != null ? new HashMap<String, EntityConfig>(loaded.monsters) : new HashMap<String, EntityConfig>();
                        ANIMAL_CONFIGS = loaded.animals != null ? new HashMap<String, EntityConfig>(loaded.animals) : new HashMap<String, EntityConfig>();
                    }
                } finally {
                    reader.close();
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.warn("Error reloading entities_voices.json: " + e.getMessage());
            }
        }
    }

    public static Set<String> getAllEntityIds() {
        HashSet<String> all = new HashSet<String>();
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
        File configDir = new File("config/ezvcsurvival");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        return new File(configDir, "entities_voices.json").toPath();
    }

    private static void loadOrCreate() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try {
                BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                try {
                    RootConfig loaded = GSON.fromJson(reader, ROOT_TYPE);
                    if (loaded != null) {
                        ROOT = loaded;
                        MONSTER_CONFIGS = loaded.monsters != null ? new HashMap<String, EntityConfig>(loaded.monsters) : new HashMap<String, EntityConfig>();
                        ANIMAL_CONFIGS = loaded.animals != null ? new HashMap<String, EntityConfig>(loaded.animals) : new HashMap<String, EntityConfig>();
                    } else {
                        ROOT = new RootConfig();
                        MONSTER_CONFIGS = new HashMap<String, EntityConfig>();
                        ANIMAL_CONFIGS = new HashMap<String, EntityConfig>();
                    }
                } finally {
                    reader.close();
                }
            } catch (JsonParseException e) {
                EZVCSurvival.LOGGER.warn("Malformed JSON in entities_voices.json, using defaults: " + e.getMessage());
                ROOT = new RootConfig();
                MONSTER_CONFIGS = new HashMap<String, EntityConfig>();
                ANIMAL_CONFIGS = new HashMap<String, EntityConfig>();
                generateDefaults();
                save(path);
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warn("Error reading entities_voices.json, regenerating: " + e.getMessage());
                ROOT = new RootConfig();
                MONSTER_CONFIGS = new HashMap<String, EntityConfig>();
                ANIMAL_CONFIGS = new HashMap<String, EntityConfig>();
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
        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValues()) {
            if (entry == null || entry.getRegistryName() == null) continue;

            String id = entry.getRegistryName().toString();
            Class<?> entityClass = entry.getEntityClass();

            if (entityClass == null) continue;

            EnumCreatureType creatureType = getCreatureType(entry);

            if (creatureType == EnumCreatureType.MONSTER) {
                if (!MONSTER_CONFIGS.containsKey(id)) {
                    MONSTER_CONFIGS.put(id, EntityConfig.defaultForMonster());
                    added = true;
                }
            } else if (isAnimalLike(creatureType)) {
                if (!ANIMAL_CONFIGS.containsKey(id)) {
                    ANIMAL_CONFIGS.put(id, EntityConfig.defaultForAnimal());
                    added = true;
                }
            }
        }
        return added;
    }

    private static EnumCreatureType getCreatureType(EntityEntry entry) {
        // En 1.12.2, EntityEntry no tiene getCategory(), así que intentamos inferirlo
        Class<?> entityClass = entry.getEntityClass();

        if (entityClass == null) return null;

        // Detectar por nombre de clase o paquete
        String className = entityClass.getName().toLowerCase();

        if (className.contains("monster") || className.contains("hostile")) {
            return EnumCreatureType.MONSTER;
        } else if (className.contains("animal") || className.contains("passive")) {
            return EnumCreatureType.CREATURE;
        } else if (className.contains("water")) {
            return EnumCreatureType.WATER_CREATURE;
        } else if (className.contains("ambient")) {
            return EnumCreatureType.AMBIENT;
        }

        return EnumCreatureType.CREATURE;
    }

    private static boolean isAnimalLike(EnumCreatureType type) {
        if (type == null) return false;
        return type == EnumCreatureType.CREATURE ||
                type == EnumCreatureType.AMBIENT ||
                type == EnumCreatureType.WATER_CREATURE;
    }

    private static void generateDefaults() {
        MONSTER_CONFIGS.clear();
        ANIMAL_CONFIGS.clear();

        for (EntityEntry entry : ForgeRegistries.ENTITIES.getValues()) {
            if (entry == null || entry.getRegistryName() == null) continue;

            String id = entry.getRegistryName().toString();
            EnumCreatureType creatureType = getCreatureType(entry);

            if (creatureType == EnumCreatureType.MONSTER) {
                MONSTER_CONFIGS.put(id, EntityConfig.defaultForMonster());
            } else if (isAnimalLike(creatureType)) {
                ANIMAL_CONFIGS.put(id, EntityConfig.defaultForAnimal());
            }
        }

        putIfPresent(MONSTER_CONFIGS, "minecraft:zombie", new EntityConfig(true, 1.7, 60.0, -20.0));
        putIfPresent(MONSTER_CONFIGS, "minecraft:skeleton", new EntityConfig(true, 1.2, 40.0, -15.0));
        putIfPresent(ANIMAL_CONFIGS, "minecraft:cow", new EntityConfig(true, 1.5, 25.0, -18.0));
        putIfPresent(ANIMAL_CONFIGS, "minecraft:pig", new EntityConfig(true, 1.2, 15.0, -18.0));
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

            BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
            try {
                GSON.toJson(ROOT, ROOT_TYPE, writer);
                writer.flush();
            } finally {
                writer.close();
            }

            if (VoiceConfig.DEBUG) {
                System.out.println("[EZVCSurvival] Successfully saved entities_voices.json with enabled=" + ROOT.enabled);
            }

        } catch (IOException e) {
            EZVCSurvival.LOGGER.error("Failed to save entities_voices.json: " + e.getMessage());
        }
    }

    public static boolean isEnabled() {
        return ROOT == null || ROOT.enabled;
    }

    public static void setEnabled(boolean enabled) {
        if (ROOT == null) ROOT = new RootConfig();
        ROOT.enabled = enabled;
        persist();

        if (VoiceConfig.DEBUG) {
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

        public static EntityConfig defaultFor(EntityEntry entry) {
            // Aquí puedes usar la lógica que necesites para generar valores predeterminados.
            EnumCreatureType creatureType = getCreatureType(entry);

            // Por ejemplo, para monstruos, puedes usar los valores predeterminados definidos previamente.
            if (creatureType == EnumCreatureType.MONSTER) {
                return defaultForMonster(); // Utiliza el valor predeterminado para monstruos.
            } else if (isAnimalLike(creatureType)) {
                return defaultForAnimal();
            }

            return new EntityConfig(false, 1.0, 50.0, -20.0);
        }


        public static EntityConfig defaultForMonster() {
            return new EntityConfig(false, 1.0, 60.0, -20.0);
        }

        public static EntityConfig defaultForAnimal() {
            return new EntityConfig(false, 1.0, 50.0, -20.0);
        }
    }

    public static final class RootConfig {
        public boolean enabled = true;
        public Map<String, EntityConfig> monsters;
        public Map<String, EntityConfig> animals;
    }
}