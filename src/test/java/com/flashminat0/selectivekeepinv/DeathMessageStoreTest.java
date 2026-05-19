package com.flashminat0.selectivekeepinv;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import static org.junit.Assert.*;

/**
 * Tests the validate-on-load behavior of {@link DeathMessageStore#fromFile}
 * plus the round-trip via {@link DeathMessageStore#writeDefaultsToFile}.
 */
public class DeathMessageStoreTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    private File freshFile(String name) throws Exception {
        return new File(tempFolder.newFolder(), name);
    }

    private static void write(File f, String body) throws Exception {
        try (Writer w = new FileWriter(f)) {
            w.write(body);
        }
    }

    // ---------------------------------------------------------------------
    // defaults()
    // ---------------------------------------------------------------------

    @Test public void defaultsReturnsNonEmptyPools() {
        DeathMessageStore d = DeathMessageStore.defaults();
        assertTrue(d.allLines.length        > 0);
        assertTrue(d.allLinesWithXp.length  > 0);
        assertTrue(d.allLinesNoXp.length    > 0);
        assertTrue(d.sameDimLines.length    > 0);
        assertTrue(d.diffDimLines.length    > 0);
        assertTrue(d.xpRollLucky.length     > 0);
        assertTrue(d.xpRollMid.length       > 0);
        assertTrue(d.xpRollBrutal.length    > 0);
    }

    @Test public void defaultsMatchDeathMessagesConstants() {
        DeathMessageStore d = DeathMessageStore.defaults();
        assertSame(DeathMessages.ALL_LINES,         d.allLines);
        assertSame(DeathMessages.ALL_LINES_WITH_XP, d.allLinesWithXp);
        assertSame(DeathMessages.SAME_DIM_LINES,    d.sameDimLines);
    }

    // ---------------------------------------------------------------------
    // writeDefaultsToFile + round-trip
    // ---------------------------------------------------------------------

    @Test public void writeAndReadBackRoundTrips() throws Exception {
        File f = freshFile("death-msgs.yml");
        DeathMessageStore.writeDefaultsToFile(f);
        assertTrue(f.exists());

        DeathMessageStore loaded = DeathMessageStore.fromFile(f);
        // Same lengths (content equality is not strictly required since
        // serialization quotes and may normalize, but they should match here).
        assertEquals(DeathMessages.ALL_LINES.length,         loaded.allLines.length);
        assertEquals(DeathMessages.ALL_LINES_WITH_XP.length, loaded.allLinesWithXp.length);
        assertEquals(DeathMessages.ALL_LINES_NO_XP.length,   loaded.allLinesNoXp.length);
        assertEquals(DeathMessages.SAME_DIM_LINES.length,    loaded.sameDimLines.length);
        assertEquals(DeathMessages.DIFF_DIM_LINES.length,    loaded.diffDimLines.length);
        assertEquals(DeathMessages.XP_ROLL_LUCKY.length,     loaded.xpRollLucky.length);
        assertEquals(DeathMessages.XP_ROLL_MID.length,       loaded.xpRollMid.length);
        assertEquals(DeathMessages.XP_ROLL_BRUTAL.length,    loaded.xpRollBrutal.length);

        // Spot-check one specific line survives the round-trip
        assertTrue(java.util.Arrays.asList(loaded.allLines)
                .contains(DeathMessages.ALL_LINES[0]));
    }

    // ---------------------------------------------------------------------
    // Validation: missing section
    // ---------------------------------------------------------------------

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void missingSectionRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        // Write a file missing xp-roll-brutal entirely
        write(f, validYamlBody().replaceFirst(
                "(?s)\\nxp-roll-brutal:\\n(\\s+- .*\\n)+", "\n"));
        DeathMessageStore.fromFile(f);
    }

    // ---------------------------------------------------------------------
    // Validation: empty pool
    // ---------------------------------------------------------------------

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void emptySectionRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        write(f, validYamlBody().replaceFirst(
                "(?s)xp-roll-mid:\\n(\\s+- .*\\n)+", "xp-roll-mid:\n"));
        DeathMessageStore.fromFile(f);
    }

    // ---------------------------------------------------------------------
    // Validation: same-dim missing %s
    // ---------------------------------------------------------------------

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void sameDimLineWithoutPercentSIsRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        // Replace the same-dim section with a line that lacks %s
        String body =
                "all-lines:\n  - \"a\"\n" +
                "all-lines-with-xp:\n  - \"a\"\n" +
                "all-lines-no-xp:\n  - \"a\"\n" +
                "same-dim-lines:\n  - \"no placeholder here\"\n" +
                "diff-dim-lines:\n  - \"a\"\n" +
                "xp-roll-lucky:\n  - \"a\"\n" +
                "xp-roll-mid:\n  - \"a\"\n" +
                "xp-roll-brutal:\n  - \"a\"\n";
        write(f, body);
        DeathMessageStore.fromFile(f);
    }

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void sameDimLineWithTwoPercentSIsRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        String body =
                "all-lines:\n  - \"a\"\n" +
                "all-lines-with-xp:\n  - \"a\"\n" +
                "all-lines-no-xp:\n  - \"a\"\n" +
                "same-dim-lines:\n  - \"%s blocks and %s extras\"\n" +
                "diff-dim-lines:\n  - \"a\"\n" +
                "xp-roll-lucky:\n  - \"a\"\n" +
                "xp-roll-mid:\n  - \"a\"\n" +
                "xp-roll-brutal:\n  - \"a\"\n";
        write(f, body);
        DeathMessageStore.fromFile(f);
    }

    // ---------------------------------------------------------------------
    // Validation: unformatted pool with %s
    // ---------------------------------------------------------------------

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void diffDimLineWithPercentSIsRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        String body =
                "all-lines:\n  - \"a\"\n" +
                "all-lines-with-xp:\n  - \"a\"\n" +
                "all-lines-no-xp:\n  - \"a\"\n" +
                "same-dim-lines:\n  - \"%s\"\n" +
                "diff-dim-lines:\n  - \"oops %s placeholder here\"\n" +
                "xp-roll-lucky:\n  - \"a\"\n" +
                "xp-roll-mid:\n  - \"a\"\n" +
                "xp-roll-brutal:\n  - \"a\"\n";
        write(f, body);
        DeathMessageStore.fromFile(f);
    }

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void allLinesEntryWithPercentDIsRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        String body =
                "all-lines:\n  - \"sneaky %d specifier\"\n" +
                "all-lines-with-xp:\n  - \"a\"\n" +
                "all-lines-no-xp:\n  - \"a\"\n" +
                "same-dim-lines:\n  - \"%s\"\n" +
                "diff-dim-lines:\n  - \"a\"\n" +
                "xp-roll-lucky:\n  - \"a\"\n" +
                "xp-roll-mid:\n  - \"a\"\n" +
                "xp-roll-brutal:\n  - \"a\"\n";
        write(f, body);
        DeathMessageStore.fromFile(f);
    }

    // ---------------------------------------------------------------------
    // Validation: malformed file
    // ---------------------------------------------------------------------

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void emptyFileIsRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        write(f, "");
        DeathMessageStore.fromFile(f);
    }

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void onlyCommentsRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        write(f, "# just a comment\n# and another\n");
        DeathMessageStore.fromFile(f);
    }

    // ---------------------------------------------------------------------
    // A minimal valid file is accepted
    // ---------------------------------------------------------------------

    @Test public void minimalValidFileAccepted() throws Exception {
        File f = freshFile("death-msgs.yml");
        String body =
                "all-lines:\n  - \"only line\"\n" +
                "all-lines-with-xp:\n  - \"only line\"\n" +
                "all-lines-no-xp:\n  - \"only line\"\n" +
                "same-dim-lines:\n  - \"only %s line\"\n" +
                "diff-dim-lines:\n  - \"only line\"\n" +
                "xp-roll-lucky:\n  - \"only line\"\n" +
                "xp-roll-mid:\n  - \"only line\"\n" +
                "xp-roll-brutal:\n  - \"only line\"\n";
        write(f, body);
        DeathMessageStore d = DeathMessageStore.fromFile(f);
        assertEquals(1, d.allLines.length);
        assertEquals("only line", d.allLines[0]);
        assertEquals("only %s line", d.sameDimLines[0]);
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    /** A minimal valid body with all sections present, used as a base for
     *  mutation tests above. */
    private static String validYamlBody() {
        return "all-lines:\n  - \"first\"\n" +
               "all-lines-with-xp:\n  - \"first\"\n" +
               "all-lines-no-xp:\n  - \"first\"\n" +
               "same-dim-lines:\n  - \"%s blocks\"\n" +
               "diff-dim-lines:\n  - \"first\"\n" +
               "xp-roll-lucky:\n  - \"first\"\n" +
               "xp-roll-mid:\n  - \"first\"\n" +
               "xp-roll-brutal:\n  - \"first\"\n";
    }
}
