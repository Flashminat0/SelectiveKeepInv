package com.flashminat0.selectivekeepinv;

import java.io.*;
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
    /**
     * When true, the mod will export the built-in message pools to
     * {@code death-msgs.yml} on first start (if not present) and load custom
     * pools from that file. When false, the file is ignored entirely.
     * If the file fails validation, this is reverted to false at runtime
     * and a warning is queued for the next op login.
     */
    public boolean overrideCornyMsgs = false;

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
            SelectiveKeepInv.reportStartupError("config.yml is malformed: " + e.getMessage()
                    + ". Using built-in defaults. Fix the file and restart to apply your changes.");
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
        messagesEnabled    = asBool(m, "enabled",             messagesEnabled);
        showXpRollFlavor   = asBool(m, "show-xp-roll-flavor", showXpRollFlavor);
        overrideCornyMsgs  = asBool(m, "override-corny-msgs", overrideCornyMsgs);
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
     * Default template written on first run. Comments and section structure
     * are literal; numeric and boolean values come from a fresh
     * {@link #defaults()} instance so the file can't drift from the field
     * initializers above.
     */
    static void writeDefault(File f) {
        Config d = defaults();
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
                "  hotbar-per-slot: " + d.hotbarPerSlot + "\n" +
                "\n" +
                "  # Cumulative XP levels to unlock each piece. Set very high (e.g.\n" +
                "  # 9999) to effectively disable that tier.\n" +
                "  offhand:        " + d.offhandThreshold + "\n" +
                "  helmet:         " + d.helmetThreshold + "\n" +
                "  chestplate:     " + d.chestplateThreshold + "\n" +
                "  leggings:       " + d.leggingsThreshold + "\n" +
                "  boots:          " + d.bootsThreshold + "\n" +
                "  accessories:    " + d.accessoriesThreshold + "\n" +
                "  main-inventory: " + d.mainInventoryThreshold + "\n" +
                "\n" +
                "  # Level at which XP carryover begins. Below this, respawn XP = 0.\n" +
                "  xp-carryover:   " + d.xpCarryoverThreshold + "\n" +
                "\n" +
                "# XP carryover gamble at level >= xp-carryover threshold.\n" +
                "# On death the divisor is rolled uniformly from [min, max] inclusive,\n" +
                "# and the player respawns with (deathLevel - threshold) / divisor XP\n" +
                "# levels. Set min = max = 1 to disable the gamble (always lucky).\n" +
                "xp-carryover:\n" +
                "  divisor-min: " + d.divisorMin + "\n" +
                "  divisor-max: " + d.divisorMax + "\n" +
                "\n" +
                "behavior:\n" +
                "  # Skip the snapshot/restore machinery for spectators.\n" +
                "  skip-spectators: " + d.skipSpectators + "\n" +
                "\n" +
                "  # In ALL mode, also cancel the XP orb drop so no XP escapes.\n" +
                "  # DEFAULT mode never cancels XP drops (the orbs are part of the\n" +
                "  # walk-back mechanic for the player).\n" +
                "  all-mode-cancels-xp-drops: " + d.allModeCancelsXpDrops + "\n" +
                "\n" +
                "messages:\n" +
                "  # Send the [SelectiveKeepInv] chat message on respawn for listed\n" +
                "  # players. Set false to suppress all chat output from the mod.\n" +
                "  enabled: " + d.messagesEnabled + "\n" +
                "\n" +
                "  # If true, send a fourth line on respawn hinting at the XP roll\n" +
                "  # outcome. Only shown when XP was actually retained.\n" +
                "  show-xp-roll-flavor: " + d.showXpRollFlavor + "\n" +
                "\n" +
                "  # If true, the mod writes its built-in message pools to\n" +
                "  # death-msgs.yml (in this same folder) on first start, then\n" +
                "  # loads custom pools from that file every start after.\n" +
                "  # Edit death-msgs.yml to use your own lines.\n" +
                "  #\n" +
                "  # If the file is invalid (missing section, empty pool, wrong\n" +
                "  # %s placeholders, malformed YAML, etc.) this flag is reverted\n" +
                "  # to false at runtime, the built-in pools are used, and a\n" +
                "  # warning is broadcast to op players on next login.\n" +
                "  override-corny-msgs: " + d.overrideCornyMsgs + "\n";

        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (Writer w = new FileWriter(f)) {
                w.write(body);
            }
        } catch (IOException e) {
            SelectiveKeepInv.reportStartupError("could not write default config.yml: " + e.getMessage());
        }
    }

}
