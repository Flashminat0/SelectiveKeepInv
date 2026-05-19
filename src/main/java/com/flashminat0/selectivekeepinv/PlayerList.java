package com.flashminat0.selectivekeepinv;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds the per-player {@link Mode}. Persisted as JSON in
 * config/selectivekeepinv/players.json.
 *
 * On-disk format:
 * <pre>
 *   {
 *     "uuid-string": "default",
 *     "uuid-string": "all"
 *   }
 * </pre>
 */
public class PlayerList {

    private static Map<UUID, Mode> players = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "players.json";

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    public static boolean contains(UUID uuid) {
        return players.containsKey(uuid);
    }

    /** Returns the mode for this UUID, or null if not on the list. */
    public static Mode getMode(UUID uuid) {
        return players.get(uuid);
    }

    /** Defensive copy of the full map. */
    public static Map<UUID, Mode> all() {
        return new HashMap<>(players);
    }

    // ---------------------------------------------------------------------
    // Mutators
    //
    // set() returns a small result describing what happened so callers can
    // print the right chat message without re-querying.
    // ---------------------------------------------------------------------

    public enum SetResult {
        /** Player was not on the list; now added with the requested mode. */
        ADDED,
        /** Player was on the list with a different mode; now switched. */
        SWITCHED,
        /** Player was already on the list with the requested mode (no-op). */
        UNCHANGED
    }

    public static SetResult set(UUID uuid, Mode mode) {
        Mode existing = players.get(uuid);
        if (existing == mode) return SetResult.UNCHANGED;
        players.put(uuid, mode);
        save();
        return existing == null ? SetResult.ADDED : SetResult.SWITCHED;
    }

    /** Remove a UUID from the list. Returns true if it was present. */
    public static boolean remove(UUID uuid) {
        boolean changed = players.remove(uuid) != null;
        if (changed) save();
        return changed;
    }

    // ---------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------

    public static void load() {
        File f = new File(SelectiveKeepInv.configDir, FILE_NAME);
        if (!f.exists()) return;
        try (Reader r = new FileReader(f)) {
            Type type = new TypeToken<Map<UUID, String>>(){}.getType();
            Map<UUID, String> loaded = GSON.fromJson(r, type);
            if (loaded != null) {
                Map<UUID, Mode> result = new HashMap<>();
                for (Map.Entry<UUID, String> e : loaded.entrySet()) {
                    Mode m = Mode.fromName(e.getValue());
                    if (m != null) result.put(e.getKey(), m);
                }
                players = result;
            }
        } catch (Exception e) {
            System.err.println("[SelectiveKeepInv] Failed to load players.json:");
            e.printStackTrace();
        }
    }

    public static void save() {
        File f = new File(SelectiveKeepInv.configDir, FILE_NAME);
        try (Writer w = new FileWriter(f)) {
            Map<UUID, String> out = new HashMap<>();
            for (Map.Entry<UUID, Mode> e : players.entrySet()) {
                out.put(e.getKey(), e.getValue().getName());
            }
            GSON.toJson(out, w);
        } catch (IOException e) {
            System.err.println("[SelectiveKeepInv] Failed to save players.json:");
            e.printStackTrace();
        }
    }
}
