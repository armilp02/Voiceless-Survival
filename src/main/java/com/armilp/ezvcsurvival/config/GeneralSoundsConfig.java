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

            if (id.startsWith("pointblank:")) {
                if (!ROOT.sounds.containsKey(id)) {
                    SoundEntry entry = new SoundEntry(false, 1.0, 1.0);

                    // Pistols
                    if (id.equals("pointblank:glock17")
                            || id.equals("pointblank:m9")
                            || id.equals("pointblank:m1911a1")
                            || id.equals("pointblank:p30l")
                            || id.equals("pointblank:deserteagle")
                            || id.equals("pointblank:rhino")) {
                        entry = new SoundEntry(true, 1.0, 4.0);
                    }

                    // Rifles
                    if (id.equals("pointblank:ak12")
                            || id.equals("pointblank:m4a1")
                            || id.equals("pointblank:m4sopmodii")
                            || id.equals("pointblank:m16a1")
                            || id.equals("pointblank:hk416")
                            || id.equals("pointblank:scarl_unsilenced")
                            || id.equals("pointblank:xm7_unsilenced")
                            || id.equals("pointblank:g36c")
                            || id.equals("pointblank:aug")
                            || id.equals("pointblank:g41")
                            || id.equals("pointblank:ak47")
                            || id.equals("pointblank:ak74")
                            || id.equals("pointblank:an94")
                            || id.equals("pointblank:ar57")
                            || id.equals("pointblank:xm29")) {
                        entry = new SoundEntry(true, 1.0, 6.5);
                    }

                    // SMG
                    if (id.equals("pointblank:mp5")
                            || id.equals("pointblank:mp7")
                            || id.equals("pointblank:ro635")
                            || id.equals("pointblank:ump45_unsilenced")
                            || id.equals("pointblank:vector")
                            || id.equals("pointblank:p90")
                            || id.equals("pointblank:m950")
                            || id.equals("pointblank:tmp")
                            || id.equals("pointblank:sl8")) {
                        entry = new SoundEntry(true, 1.0, 3.0);
                    }

                    // Snipers
                    if (id.equals("pointblank:mk14ebr")
                            || id.equals("pointblank:uar10")
                            || id.equals("pointblank:g3")
                            || id.equals("pointblank:wa2000")
                            || id.equals("pointblank:xm3")
                            || id.equals("pointblank:l96a1")
                            || id.equals("pointblank:ballista")
                            || id.equals("pointblank:gm6lynx")) {
                        entry = new SoundEntry(true, 1.0, 5.0);
                    }

                    // Shotguns
                    if (id.equals("pointblank:m590")
                            || id.equals("pointblank:m870")
                            || id.equals("pointblank:spas12")
                            || id.equals("pointblank:aa12")
                            || id.equals("pointblank:citoricxs")
                            || id.equals("pointblank:hs12")) {
                        entry = new SoundEntry(true, 1.0, 4.8);
                    }

                    // RPG
                    if (id.equals("pointblank:mgl_shoot")
                            || id.equals("pointblank:launcher")
                            || id.equals("pointblank:at4")) {
                        entry = new SoundEntry(true, 1.0, 10.0);
                    }

                    // MG
                    if (id.equals("pointblank:lamg")
                            || id.equals("pointblank:mk48")
                            || id.equals("pointblank:m249")
                            || id.equals("pointblank:m134minigun")) {
                        entry = new SoundEntry(true, 1.0, 8.0);
                    }

                    ROOT.sounds.put(id, entry);
                    changed = true;
                }
            } else {
                if (!ROOT.sounds.containsKey(id)) {
                    ROOT.sounds.put(id, new SoundEntry(true, 1.0, 1.0));
                    changed = true;
                }
            }


            if (id.contains("step") || id.contains("ambient") || id.contains("cave")
                    || id.contains("idle") || id.contains("music") || id.contains("weather")
                    || id.contains("ui") || id.contains("furnace") || id.contains("equip")
                    || id.contains("tacz") || id.contains("pickup") || id.contains("drop")
                    || id.contains("hit"))  {
                SoundEntry soundEntry = ROOT.sounds.get(id);
                if (soundEntry != null && soundEntry.enabled) {
                    soundEntry.enabled = false;
                    changed = true;
                }
            }
        }
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

    // ==== CLASES DE DATOS ====
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
