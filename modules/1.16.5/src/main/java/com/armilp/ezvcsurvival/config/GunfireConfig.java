package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.EZVCSurvival;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
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
    private static final Type ROOT_TYPE = new TypeToken<Root>() {
    }.getType();

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
                String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                java.io.StringReader r = new java.io.StringReader(content);
                try {
                    Root loaded = GSON.fromJson(r, ROOT_TYPE);
                    if (loaded != null) {
                        ROOT = loaded;
                        if (ROOT.mobs == null) ROOT.mobs = new HashMap<String, Reaction>();
                    }
                } finally {
                    r.close();
                }
            } catch (Exception e) {
                EZVCSurvival.LOGGER.warning("Error reloading gunfire.json: {}");
            }
        }
    }

    public static Map<String, Reaction> getMobReactions() {
        if (ROOT == null) ROOT = new Root();
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<String, Reaction>();
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
                String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                java.io.StringReader r = new java.io.StringReader(content);
                try {
                    Root loaded = GSON.fromJson(r, ROOT_TYPE);
                    ROOT = loaded != null ? loaded : defaultRoot();
                } finally {
                    r.close();
                }
            } catch (MalformedJsonException e) {
                EZVCSurvival.LOGGER.warning("Malformed JSON in gunfire.json, using defaults (no backup): {}");
                ROOT = defaultRoot();
                isNewFile = true;
                save(path);
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warning("Error reading gunfire.json, regenerating: {}");
                ROOT = defaultRoot();
                isNewFile = true;
                save(path);
            }
        } else {
            ROOT = defaultRoot();
        }

        boolean changed = ensureAllPresent(isNewFile);
        if (changed) save(path);
    }

    private static boolean ensureAllPresent(boolean isNewFile) {
        boolean changed = false;
        if (ROOT.mobs == null) {
            ROOT.mobs = new HashMap<String, Reaction>();
            changed = true;
        }

        for (EntityType<?> type : ForgeRegistries.ENTITIES) {
            EntityClassification cat = type.getCategory();
            if (cat == EntityClassification.MISC) continue;
            String id = Objects.requireNonNull(ForgeRegistries.ENTITIES.getKey(type)).toString();

            if (!ROOT.mobs.containsKey(id)) {
                boolean shouldEnable = isNewFile && (
                        id.equals("minecraft:zombie") ||
                                id.equals("minecraft:skeleton") ||
                                id.equals("minecraft:cow") ||
                                id.equals("minecraft:pig") ||
                                id.startsWith("zombie_extreme:") ||
                                id.startsWith("apocalypsenow:")
                );

                double defaultRange = cat == EntityClassification.MONSTER ? 50.0 : 40.0;
                ROOT.mobs.put(id, new Reaction(shouldEnable, 1.0, defaultRange));
                changed = true;
            }
        }

        return changed;
    }

    private static Root defaultRoot() {
        Root r = new Root();
        r.mobs = new HashMap<String, Reaction>();
        return r;
    }

    private static void save(Path path) {
        if (ROOT == null) {
            EZVCSurvival.LOGGER.warning("Cannot save null ROOT configuration");
            return;
        }

        try {
            Files.createDirectories(path.getParent());

            BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            try {
                GSON.toJson(ROOT, ROOT_TYPE, w);
                w.flush();
            } finally {
                w.close();
            }

            if (VoiceConfig.DEBUG.get()) {
                System.out.println("[EZVCSurvival] Successfully saved gunfire.json");
            }

        } catch (IOException e) {
            EZVCSurvival.LOGGER.severe("Failed to save gunfire.json: {}");
        }
    }

    public static void setMobReaction(String entityId, boolean enabled, double speed, double range) {
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<String, Reaction>();
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