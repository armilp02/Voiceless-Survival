package com.armilp.ezvcsurvival.config;

import com.armilp.ezvcsurvival.compat.pointblank.PointBlankSoundsConfig;
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
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Objects;

public final class GeneralSoundsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_TYPE = new TypeToken<Root>() {}.getType();

    public static Root ROOT = new Root();

    private GeneralSoundsConfig() {}

    public static void init() { loadOrCreate(); }

    public static Map<String, Reaction> getMobReactions() { return ROOT.mobs; }
    public static Map<String, SoundEntry> getSounds() { return ROOT.sounds; }
    public static boolean isEnabled() { return ROOT != null ? ROOT.enabled : true; }

    public static void setMobReaction(String entityId, boolean enabled, double speed, double range) {
        if (ROOT.mobs == null) ROOT.mobs = new HashMap<>();
        ROOT.mobs.put(entityId, new Reaction(enabled, speed, range));
    }

    public static void setSoundEntry(String soundId, boolean enabled, double speedMultiplier, double rangeMultiplier) {
        if (ROOT.sounds == null) ROOT.sounds = new HashMap<>();
        ROOT.sounds.put(soundId, new SoundEntry(enabled, speedMultiplier, rangeMultiplier));
    }

    public static void persist() {
        Path path = getPath();
        save(path);
    }

    private static Path getPath() {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("ezvcsurvival");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir.resolve("generalsounds.json");
    }

    private static void loadOrCreate() {
        Path path = getPath();
        if (Files.exists(path)) {
            try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Root loaded = GSON.fromJson(r, ROOT_TYPE);
                ROOT = loaded != null ? loaded : defaultRoot();
            } catch (IOException e) {
                EZVCSurvival.LOGGER.warn("Error leyendo generalsounds.json, regenerando: {}", e.getMessage());
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
        if (ROOT.sounds == null) { ROOT.sounds = new HashMap<>(); changed = true; }

        // ENTITIES
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            MobCategory cat = type.getCategory();
            if (cat == MobCategory.MISC) continue;
            String id = Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(type)).toString();
            if (!ROOT.mobs.containsKey(id)) {
                ROOT.mobs.put(id, new Reaction(true, 1.0, cat == MobCategory.MONSTER ? 60.0 : 50.0));
                changed = true;
            }
        }

        // SOUNDS
        for (var sound : BuiltInRegistries.SOUND_EVENT) {
            String id = Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(sound)).toString();
            if (!ROOT.sounds.containsKey(id)) {
                boolean isVanilla = id.startsWith("minecraft:");
                ROOT.sounds.put(id, new SoundEntry(isVanilla, 1.0, 1.0));
                changed = true;
            }

            if (!id.startsWith("pointblank:")) {
                SoundEntry soundEntry = ROOT.sounds.get(id);
                if (id.contains("place") || id.contains("break") || id.contains("door") || id.contains("chest")
                        || id.contains("pressure_plate") || id.contains("tripwire") || id.contains("dispenser")
                        || id.contains("anvil")) {
                    if (soundEntry != null && !soundEntry.enabled) {
                        soundEntry.enabled = true;
                        changed = true;
                    }
                } else {
                    if (soundEntry != null && soundEntry.enabled) {
                        soundEntry.enabled = false;
                        changed = true;
                    }
                }
            }
        }

        PointBlankSoundsConfig.apply(ROOT.sounds);

        return changed;
    }

    private static Root defaultRoot() {
        Root r = new Root();
        r.mobs = new HashMap<>();
        r.sounds = new HashMap<>();
        return r;
    }

    private static void save(Path path) {
        try {
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
            try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(ROOT, ROOT_TYPE, w);
            }
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            EZVCSurvival.LOGGER.warn("No se pudo guardar generalsounds.json: {}", e.getMessage());
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

        public Reaction(boolean enabled, double speed, double range) {
            this.enabled = enabled;
            this.speed = speed;
            this.range = range;
        }
    }

    public static final class SoundEntry {
        public boolean enabled;
        public double speed_multiplier;
        public double range_multiplier;

        public SoundEntry(boolean enabled, double speedMultiplier, double rangeMultiplier) {
            this.enabled = enabled;
            this.speed_multiplier = speedMultiplier;
            this.range_multiplier = rangeMultiplier;
        }
    }
}
