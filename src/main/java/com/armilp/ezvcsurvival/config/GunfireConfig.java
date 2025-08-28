package com.armilp.ezvcsurvival.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.armilp.ezvcsurvival.EZVCSurvival;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Objects;

public final class GunfireConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_TYPE = new TypeToken<Root>() {}.getType();

    public static Root ROOT = new Root();

    private GunfireConfig() {}

    public static void init() { loadOrCreate(); }
    public static Map<String, Reaction> getMobReactions() { return ROOT.mobs; }
    public static Map<String, Boolean> getGunPrioritySounds() { return ROOT.priority_sounds; }
    public static boolean isEnabled() { return ROOT != null ? ROOT.enabled : true; }

    private static Path getPath() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("ezvcsurvival");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir.resolve("gunfire.json");
    }

    private static void loadOrCreate() {
        Path path = getPath();
        if (Files.exists(path)) {
            try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Root loaded = GSON.fromJson(r, ROOT_TYPE);
                ROOT = loaded != null ? loaded : defaultRoot();
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warn("Error leyendo gunfire.json, regenerando: {}", e.getMessage());
                ROOT = defaultRoot();
                save(path);
            }
        } else {
            ROOT = defaultRoot();
            save(path);
        }

        boolean changed = ensureAllPresent();
        if (changed) save(path);
    }

    private static boolean ensureAllPresent() {
        boolean changed = false;
        if (ROOT.mobs == null) { ROOT.mobs = new HashMap<>(); changed = true; }
        if (ROOT.priority_sounds == null) { ROOT.priority_sounds = new HashMap<>(); changed = true; }

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory cat = type.getCategory();
            if (cat == MobCategory.MISC) continue;
            String id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
            if (!ROOT.mobs.containsKey(id)) {
                ROOT.mobs.put(id, new Reaction(true, 1.0, cat == MobCategory.MONSTER ? 50.0 : 40.0));
                changed = true;
            }
        }

        for (var sound : BuiltInRegistries.SOUND_EVENT) {
            String id = Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(sound)).toString();
            if (!ROOT.priority_sounds.containsKey(id)) {
                ROOT.priority_sounds.put(id, Boolean.FALSE);
                changed = true;
            }
        }
        return changed;
    }

    private static Root defaultRoot() {
        Root r = new Root();
        r.mobs = new HashMap<>();
        r.priority_sounds = new HashMap<>();
        return r;
    }

    private static void save(Path path) {
        try {
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(ROOT, ROOT_TYPE, w);
            }
            Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            EZVCSurvival.LOGGER.warn("No se pudo guardar gunfire.json: {}", e.getMessage());
        }
    }

    public static void setMobReaction(String entityId, boolean enabled, double speed, double range) {
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<>();
        ROOT.mobs.put(entityId, new Reaction(enabled, speed, range));
    }
    public static void setPrioritySound(String soundId, boolean enabled) {
        if (ROOT.priority_sounds == null) ROOT.priority_sounds = new HashMap<>();
        ROOT.priority_sounds.put(soundId, enabled);
    }
    public static void persist() { save(getPath()); }


    public static final class Root {
        public boolean enabled = true;
        public Map<String, Reaction> mobs;
        public Map<String, Boolean> priority_sounds;
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