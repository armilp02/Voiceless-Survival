package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class GunfireConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_TYPE = new TypeToken<Root>() {}.getType();

    public static Root ROOT = new Root();
    private static boolean isInitialized = false;

    private GunfireConfig() {
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
                String content = Files.readString(path, StandardCharsets.UTF_8);
                try (java.io.StringReader r = new java.io.StringReader(content)) {
                    Root loaded = GSON.fromJson(r, ROOT_TYPE);
                    if (loaded != null) {
                        ROOT = loaded;
                        if (ROOT.mobs == null) ROOT.mobs = new HashMap<>();

                        if (SoundConfig.isDebugEnabled()) {
                            EZVCSurvival.LOGGER.info("[GunfireConfig] Reloaded {} mob reactions", ROOT.mobs.size());
                        }
                    }
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.warn("Error reloading gunfire.json: {}", e.getMessage());
            }
        }
    }

    public static Map<String, Reaction> getMobReactions() {
        if (ROOT == null) ROOT = new Root();
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<>();
        return ROOT.mobs;
    }

    public static boolean isEnabled() {
        return ROOT == null || ROOT.enabled;
    }

    private static Path getPath() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("ezvcsurvival");
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir.resolve("gunfire.json");
    }

    private static void loadOrCreate() {
        Path path = getPath();
        boolean isNewFile = !Files.exists(path);

        if (!isNewFile) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                try (java.io.StringReader r = new java.io.StringReader(content)) {
                    Root loaded = GSON.fromJson(r, ROOT_TYPE);
                    ROOT = loaded != null ? loaded : defaultRoot();

                    if (SoundConfig.isDebugEnabled()) {
                        EZVCSurvival.LOGGER.info("[GunfireConfig] Loaded {} mob reactions from file",
                                ROOT.mobs != null ? ROOT.mobs.size() : 0);
                    }
                }
            } catch (MalformedJsonException e) {
                EZVCSurvival.LOGGER.warn("Malformed JSON in gunfire.json, using defaults: {}", e.getMessage());
                ROOT = defaultRoot();
                isNewFile = true;
                save(path);
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warn("Error reading gunfire.json, regenerating: {}", e.getMessage());
                ROOT = defaultRoot();
                isNewFile = true;
                save(path);
            }
        } else {
            ROOT = defaultRoot();
        }

        if (SoundConfig.ENABLE_GUNFIRE.get()) {
            boolean changed = ensureAllPresent(isNewFile);
            if (changed) {
                save(path);
                if (SoundConfig.isDebugEnabled()) {
                    EZVCSurvival.LOGGER.info("[GunfireConfig] Auto-generation enabled: added new entities");
                }
            }
        } else {
            if (SoundConfig.isDebugEnabled()) {
                EZVCSurvival.LOGGER.info("[GunfireConfig] Auto-generation disabled: using existing entities only");
            }
        }
    }

    private static boolean ensureAllPresent(boolean isNewFile) {
        boolean changed = false;
        if (ROOT.mobs == null) {
            ROOT.mobs = new HashMap<>();
            changed = true;
        }

        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            MobCategory cat = type.getCategory();
            if (cat == MobCategory.MISC) continue;
            String id = Objects.requireNonNull(ForgeRegistries.ENTITY_TYPES.getKey(type)).toString();

            if (!ROOT.mobs.containsKey(id)) {
                boolean shouldEnable = isNewFile && (
                        id.equals("minecraft:zombie") ||
                                id.equals("minecraft:skeleton") ||
                                id.equals("minecraft:cow") ||
                                id.equals("minecraft:pig") ||
                                id.startsWith("zombie_extreme:") ||
                                id.startsWith("apocalypsenow:")
                );

                double defaultRange = cat == MobCategory.MONSTER ? 50.0 : 40.0;
                ROOT.mobs.put(id, new Reaction(shouldEnable, 1.0, defaultRange));
                changed = true;
            }
        }

        return changed;
    }

    private static Root defaultRoot() {
        Root r = new Root();
        r.mobs = new HashMap<>();
        return r;
    }

    private static void save(Path path) {
        if (ROOT == null) {
            EZVCSurvival.LOGGER.warn("Cannot save null ROOT configuration");
            return;
        }

        try {
            Files.createDirectories(path.getParent());

            try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(ROOT, ROOT_TYPE, w);
                w.flush();
            }

            if (SoundConfig.isDebugEnabled()) {
                EZVCSurvival.LOGGER.info("[GunfireConfig] Saved config with {} entries",
                        ROOT.mobs != null ? ROOT.mobs.size() : 0);
            }

        } catch (IOException e) {
            EZVCSurvival.LOGGER.error("Failed to save gunfire.json: {}", e.getMessage());
        }
    }

    public static void setMobReaction(String entityId, boolean enabled, double speed, double range) {
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<>();
        ROOT.mobs.put(entityId, new Reaction(enabled, speed, range));
    }

    public static void persist() {
        save(getPath());
    }

    public static final class Root {
        public boolean enabled = true;
        public Map<String, Reaction> mobs;
    }

    public static final class Reaction {
        public boolean enabled;
        public double speed;
        public double range;

        public Reaction(boolean enabled, double speed, double range) {
            this.enabled = enabled;
            this.speed = speed;
            this.range = range;
        }
    }
}