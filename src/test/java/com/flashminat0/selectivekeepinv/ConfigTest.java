package com.flashminat0.selectivekeepinv;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import static org.junit.Assert.*;

/**
 * Covers {@link Config} loading, default-writing, and graceful handling of
 * partial / malformed YAML.
 */
public class ConfigTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    private File configDir() throws Exception {
        return tempFolder.newFolder("cfg");
    }

    // ---------------------------------------------------------------------
    // Defaults
    // ---------------------------------------------------------------------

    @Test public void defaultsMatchV11HardcodedValues() {
        Config c = Config.defaults();
        assertEquals(1,    c.hotbarPerSlot);
        assertEquals(10,   c.offhandThreshold);
        assertEquals(11,   c.helmetThreshold);
        assertEquals(16,   c.chestplateThreshold);
        assertEquals(21,   c.leggingsThreshold);
        assertEquals(26,   c.bootsThreshold);
        assertEquals(50,   c.accessoriesThreshold);
        assertEquals(100,  c.mainInventoryThreshold);
        assertEquals(100,  c.xpCarryoverThreshold);
        assertEquals(1,    c.divisorMin);
        assertEquals(3,    c.divisorMax);
        assertTrue (c.skipSpectators);
        assertTrue (c.allModeCancelsXpDrops);
        assertTrue (c.messagesEnabled);
        assertTrue (c.showXpRollFlavor);
    }

    // ---------------------------------------------------------------------
    // First-run: writes default file, returns defaults instance
    // ---------------------------------------------------------------------

    @Test public void loadOnMissingFileWritesDefaultAndReturnsDefaults() throws Exception {
        File dir = configDir();
        File file = new File(dir, "config.yml");
        assertFalse(file.exists());

        Config c = Config.load(dir);

        assertTrue("config.yml should be created on first load", file.exists());
        assertEquals(10,  c.offhandThreshold);
        assertEquals(100, c.mainInventoryThreshold);
    }

    @Test public void writtenDefaultFileIsParsable() throws Exception {
        File dir = configDir();
        Config.load(dir);  // writes the default
        // Loading again should now read the file (not re-write), get the same values.
        Config c2 = Config.load(dir);
        assertEquals(10,  c2.offhandThreshold);
        assertEquals(50,  c2.accessoriesThreshold);
        assertEquals(3,   c2.divisorMax);
        assertTrue (c2.skipSpectators);
    }

    // ---------------------------------------------------------------------
    // Full valid file
    // ---------------------------------------------------------------------

    @Test public void loadFullValidFileUsesValues() throws Exception {
        File dir = configDir();
        writeYaml(dir,
                "thresholds:\n" +
                "  hotbar-per-slot: 2\n" +
                "  offhand: 50\n" +
                "  helmet: 60\n" +
                "  chestplate: 70\n" +
                "  leggings: 80\n" +
                "  boots: 90\n" +
                "  accessories: 100\n" +
                "  main-inventory: 200\n" +
                "  xp-carryover: 200\n" +
                "xp-carryover:\n" +
                "  divisor-min: 2\n" +
                "  divisor-max: 5\n" +
                "behavior:\n" +
                "  skip-spectators: false\n" +
                "  all-mode-cancels-xp-drops: false\n" +
                "messages:\n" +
                "  enabled: false\n" +
                "  show-xp-roll-flavor: false\n"
        );

        Config c = Config.load(dir);
        assertEquals(2,    c.hotbarPerSlot);
        assertEquals(50,   c.offhandThreshold);
        assertEquals(60,   c.helmetThreshold);
        assertEquals(70,   c.chestplateThreshold);
        assertEquals(80,   c.leggingsThreshold);
        assertEquals(90,   c.bootsThreshold);
        assertEquals(100,  c.accessoriesThreshold);
        assertEquals(200,  c.mainInventoryThreshold);
        assertEquals(200,  c.xpCarryoverThreshold);
        assertEquals(2,    c.divisorMin);
        assertEquals(5,    c.divisorMax);
        assertFalse(c.skipSpectators);
        assertFalse(c.allModeCancelsXpDrops);
        assertFalse(c.messagesEnabled);
        assertFalse(c.showXpRollFlavor);
    }

    // ---------------------------------------------------------------------
    // Partial file: missing fields use defaults
    // ---------------------------------------------------------------------

    @Test public void partialFileFallsBackToDefaultsPerField() throws Exception {
        File dir = configDir();
        writeYaml(dir,
                "thresholds:\n" +
                "  offhand: 99\n" +
                "messages:\n" +
                "  enabled: false\n"
        );

        Config c = Config.load(dir);
        // changed
        assertEquals(99, c.offhandThreshold);
        assertFalse(c.messagesEnabled);
        // not specified, stays default
        assertEquals(11,  c.helmetThreshold);
        assertEquals(100, c.xpCarryoverThreshold);
        assertEquals(3,   c.divisorMax);
        assertTrue (c.skipSpectators);
        assertTrue (c.showXpRollFlavor);
    }

    @Test public void missingSectionsAreDefaults() throws Exception {
        File dir = configDir();
        writeYaml(dir, "messages:\n  enabled: false\n");

        Config c = Config.load(dir);
        assertFalse(c.messagesEnabled);
        // Entire thresholds section absent: all defaults
        assertEquals(10,  c.offhandThreshold);
        assertEquals(11,  c.helmetThreshold);
        assertEquals(26,  c.bootsThreshold);
    }

    // ---------------------------------------------------------------------
    // Malformed input falls back to fresh defaults
    // ---------------------------------------------------------------------

    @Test public void malformedYamlFallsBackToDefaults() throws Exception {
        File dir = configDir();
        writeYaml(dir, "this is not\n  remotely valid\n: : : nonsense\n");

        Config c = Config.load(dir);
        // Should return defaults; mod still loads.
        assertEquals(10, c.offhandThreshold);
        assertEquals(11, c.helmetThreshold);
        assertTrue(c.skipSpectators);
    }

    @Test public void emptyFileFallsBackToDefaults() throws Exception {
        File dir = configDir();
        writeYaml(dir, "");

        Config c = Config.load(dir);
        assertEquals(10, c.offhandThreshold);
        assertTrue (c.messagesEnabled);
    }

    // ---------------------------------------------------------------------
    // Comments and blank lines are ignored
    // ---------------------------------------------------------------------

    @Test public void commentsAndBlankLinesIgnored() throws Exception {
        File dir = configDir();
        writeYaml(dir,
                "# top comment\n" +
                "\n" +
                "thresholds:\n" +
                "  # inline comment above field\n" +
                "  offhand: 42\n" +
                "\n" +
                "  helmet: 77 # trailing comment\n" +
                "\n"
        );

        Config c = Config.load(dir);
        assertEquals(42, c.offhandThreshold);
        assertEquals(77, c.helmetThreshold);
    }

    // ---------------------------------------------------------------------
    // Edge values are tolerated (handled at use site)
    // ---------------------------------------------------------------------

    @Test public void negativeAndZeroValuesAreLoadedRaw() throws Exception {
        File dir = configDir();
        writeYaml(dir,
                "thresholds:\n" +
                "  hotbar-per-slot: 0\n" +
                "  offhand: -5\n" +
                "xp-carryover:\n" +
                "  divisor-min: 0\n" +
                "  divisor-max: -1\n"
        );

        Config c = Config.load(dir);
        // Loading is faithful; clamping happens in PreservationPlan and
        // EventHandler.rollDivisor at use time.
        assertEquals(0,  c.hotbarPerSlot);
        assertEquals(-5, c.offhandThreshold);
        assertEquals(0,  c.divisorMin);
        assertEquals(-1, c.divisorMax);
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static void writeYaml(File dir, String body) throws Exception {
        File f = new File(dir, "config.yml");
        try (Writer w = new FileWriter(f)) {
            w.write(body);
        }
    }
}
