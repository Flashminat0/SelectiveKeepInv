package com.flashminat0.selectivekeepinv;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime configuration loaded from {@code config/selectivekeepinv/config.yml}
 * at mod preInit. All fields ship with sensible defaults matching the v1.1
 * hardcoded behavior; if the YAML file doesn't exist on first start, the
 * defaults below are used and a commented file is written next to
 * {@code players.json} for the user to edit.
 *
 * <p>YAML is parsed by the in-house {@link SimpleYaml} reader. The schema is
 * just two levels of scalar key/value, so this is a few-dozen lines of code
 * instead of a 300 KB shaded SnakeYAML.
 *
 * <p>Missing fields fall back to defaults silently. Malformed YAML logs a
 * stack trace and falls back to a fresh defaults instance (the mod still
 * loads cleanly).
 */
public class Config {

    // -----------------------------------------------------------------
    // Threshold settings (DEFAULT-mode preservation tiers)
    // -----------------------------------------------------------------

    /** XP level cost per hotbar slot (clamped to >=1 at use site). */
    public int hotbarPerSlot = 1;

    public int offhandThreshold        = 10;
    public int helmetThreshold         = 11;
    public int chestplateThreshold     = 16;
    public int leggingsThreshold       = 21;
    public int bootsThreshold          = 26;
    public int accessoriesThreshold    = 50;
    public int mainInventoryThreshold  = 100;
    public int xpCarryoverThreshold    = 100;

    // -----------------------------------------------------------------
    // XP carryover divisor range
    // -----------------------------------------------------------------

    /** Inclusive lower bound for the per-death divisor roll. */
    public int divisorMin = 1;
    /** Inclusive upper bound for the per-death divisor roll. */
    public int divisorMax = 3;

    // -----------------------------------------------------------------
    // Behavior toggles
    // -----------------------------------------------------------------

    public boolean skipSpectators           = true;
    public boolean allModeCancelsXpDrops    = true;

    // -----------------------------------------------------------------
    // Message toggles
    // -----------------------------------------------------------------

    public boolean messagesEnabled  = true;
    public boolean showXpRollFlavor = true;

    // -----------------------------------------------------------------
    // I/O
    // -----------------------------------------------------------------

    private static final String FILE_NAME = "config.yml";

    /** Returns a fresh defaults instance. Handy for tests. */
    public static Config defaults() {
        return new Config();
    }

    /**
     * Load config from configDir/config.yml. If the file doesn't exist, write
     * defaults to it and return a defaults instance. If it exists but is
     * malformed, log and return defaults.
     */
    public static Config load(File configDir) {
        File f = new File(configDir, FILE_NAME);
        if (!f.exists()) {
            writeDefault(f);
            return new Config();
        }
        Config cfg = new Config();
        try (Reader r = new FileReader(f)) {
            Map<String, Map<String, Object>> root = SimpleYaml.parse(r);
            applySection(root.get("thresholds"),  cfg::applyThresholds);
            applySection(root.get("xp-carryover"), cfg::applyXpCarryover);
            applySection(root.get("behavior"),     cfg::applyBehavior);
            applySection(root.get("messages"),     cfg::applyMessages);
        } catch (Exception e) {
            System.err.println("[SelectiveKeepInv] Failed to load config.yml, using defaults:");
            e.printStackTrace();
            return new Config();
        }
        return cfg;
    }

    private interface SectionApplier { void apply(Map<String, Object> section); }

    private static void applySection(Map<String, Object> section, SectionApplier f) {
        if (section != null) f.apply(section);
    }

    private void applyThresholds(Map<String, Object> m) {
        hotbarPerSlot          = asInt(m, "hotbar-per-slot",  hotbarPerSlot);
        offhandThreshold       = asInt(m, "offhand",          offhandThreshold);
        helmetThreshold        = asInt(m, "helmet",           helmetThreshold);
        chestplateThreshold    = asInt(m, "chestplate",       chestplateThreshold);
        leggingsThreshold      = asInt(m, "leggings",         leggingsThreshold);
        bootsThreshold         = asInt(m, "boots",            bootsThreshold);
        accessoriesThreshold   = asInt(m, "accessories",      accessoriesThreshold);
        mainInventoryThreshold = asInt(m, "main-inventory",   mainInventoryThreshold);
        xpCarryoverThreshold   = asInt(m, "xp-carryover",     xpCarryoverThreshold);
    }

    private void applyXpCarryover(Map<String, Object> m) {
        divisorMin = asInt(m, "divisor-min", divisorMin);
        divisorMax = asInt(m, "divisor-max", divisorMax);
    }

    private void applyBehavior(Map<String, Object> m) {
        skipSpectators        = asBool(m, "skip-spectators",            skipSpectators);
        allModeCancelsXpDrops = asBool(m, "all-mode-cancels-xp-drops",  allModeCancelsXpDrops);
    }

    private void applyMessages(Map<String, Object> m) {
        messagesEnabled  = asBool(m, "enabled",             messagesEnabled);
        showXpRollFlavor = asBool(m, "show-xp-roll-flavor", showXpRollFlavor);
    }

    private static int asInt(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    private static boolean asBool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return def;
    }

    /**
     * Hardcoded default template written on first run. Comments are baked in
     * so users see explanations next to the values. Keep in sync with field
     * defaults above.
     */
    static void writeDefault(File f) {
        String body =
                "# Selective Keep Inventory configuration\n" +
                "# Generated on first run. Edit and restart the server to apply.\n" +
                "# Missing fields fall back to defaults silently.\n" +
                "\n" +
                "# DEFAULT-mode XP-cost thresholds. Each entry is the XP level at\n" +
                "# which that thing starts being preserved on death. Cumulative.\n" +
                "thresholds:\n" +
                "  # XP cost per hotbar slot, leftmost first.\n" +
                "  # Cost = 1 means slot 0 at level 1, slot 8 at level 9.\n" +
                "  hotbar-per-slot: 1\n" +
                "\n" +
                "  # Cumulative XP levels to unlock each piece. Set very high (e.g.\n" +
                "  # 9999) to effectively disable that tier.\n" +
                "  offhand:        10\n" +
                "  helmet:         11\n" +
                "  chestplate:     16\n" +
                "  leggings:       21\n" +
                "  boots:          26\n" +
                "  accessories:    50\n" +
                "  main-inventory: 100\n" +
                "\n" +
                "  # Level at which XP carryover begins. Below this, respawn XP = 0.\n" +
                "  xp-carryover:   100\n" +
                "\n" +
                "# XP carryover gamble at level >= xp-carryover threshold.\n" +
                "# On death the divisor is rolled uniformly from [min, max] inclusive,\n" +
                "# and the player respawns with (deathLevel - threshold) / divisor XP\n" +
                "# levels. Set min = max = 1 to disable the gamble (always lucky).\n" +
                "xp-carryover:\n" +
                "  divisor-min: 1\n" +
                "  divisor-max: 3\n" +
                "\n" +
                "behavior:\n" +
                "  # Skip the snapshot/restore machinery for spectators.\n" +
                "  skip-spectators: true\n" +
                "\n" +
                "  # In ALL mode, also cancel the XP orb drop so no XP escapes.\n" +
                "  # DEFAULT mode never cancels XP drops (the orbs are part of the\n" +
                "  # walk-back mechanic for the player).\n" +
                "  all-mode-cancels-xp-drops: true\n" +
                "\n" +
                "messages:\n" +
                "  # Send the [SelectiveKeepInv] chat message on respawn for listed\n" +
                "  # players. Set false to suppress all chat output from the mod.\n" +
                "  enabled: true\n" +
                "\n" +
                "  # If true, send a fourth line on respawn hinting at the XP roll\n" +
                "  # outcome. Only shown when XP was actually retained.\n" +
                "  show-xp-roll-flavor: true\n";

        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (Writer w = new FileWriter(f)) {
                w.write(body);
            }
        } catch (IOException e) {
            System.err.println("[SelectiveKeepInv] Failed to write default config.yml:");
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------
    // Tiny YAML parser for our 2-level scalar schema.
    //
    // Handles: comments (#), blank lines, top-level "key:" sections,
    //          indented "key: value" pairs with int / bool / string values.
    // Quoted strings: optional "..." or '...' wrapping is stripped.
    // -----------------------------------------------------------------
    static class SimpleYaml {

        static Map<String, Map<String, Object>> parse(Reader reader) throws IOException {
            Map<String, Map<String, Object>> root = new LinkedHashMap<>();
            Map<String, Object> currentSection = null;
            BufferedReader br = new BufferedReader(reader);
            String raw;
            while ((raw = br.readLine()) != null) {
                String line = stripComment(raw);
                if (line.trim().isEmpty()) continue;

                boolean indented = line.length() > 0 && Character.isWhitespace(line.charAt(0));
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();

                if (!indented) {
                    currentSection = new LinkedHashMap<>();
                    root.put(key, currentSection);
                } else {
                    if (currentSection == null) continue;
                    currentSection.put(key, parseScalar(value));
                }
            }
            return root;
        }

        private static String stripComment(String s) {
            // Naive: comments don't appear inside our values (no strings with #).
            int hash = s.indexOf('#');
            return hash < 0 ? s : s.substring(0, hash);
        }

        private static Object parseScalar(String s) {
            if (s.isEmpty()) return s;
            if (s.equals("true"))  return Boolean.TRUE;
            if (s.equals("false")) return Boolean.FALSE;
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {}
            if (s.length() >= 2) {
                char first = s.charAt(0), last = s.charAt(s.length() - 1);
                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    return s.substring(1, s.length() - 1);
                }
            }
            return s;
        }
    }
}
