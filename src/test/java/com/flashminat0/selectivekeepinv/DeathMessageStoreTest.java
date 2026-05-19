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
    // writeDefaultsToFile + round-trip — every line must come back identical
    // ---------------------------------------------------------------------

    @Test public void writeAndReadBackRoundTripsExactly() throws Exception {
        File f = freshFile("death-msgs.yml");
        DeathMessageStore.writeDefaultsToFile(f);
        assertTrue(f.exists());

        DeathMessageStore loaded = DeathMessageStore.fromFile(f);

        // Strict equality on every pool. Catches quote/escape mutations or
        // a line being silently dropped during serialization.
        assertArrayEquals(DeathMessages.ALL_LINES,         loaded.allLines);
        assertArrayEquals(DeathMessages.ALL_LINES_WITH_XP, loaded.allLinesWithXp);
        assertArrayEquals(DeathMessages.ALL_LINES_NO_XP,   loaded.allLinesNoXp);
        assertArrayEquals(DeathMessages.SAME_DIM_LINES,    loaded.sameDimLines);
        assertArrayEquals(DeathMessages.DIFF_DIM_LINES,    loaded.diffDimLines);
        assertArrayEquals(DeathMessages.XP_ROLL_LUCKY,     loaded.xpRollLucky);
        assertArrayEquals(DeathMessages.XP_ROLL_MID,       loaded.xpRollMid);
        assertArrayEquals(DeathMessages.XP_ROLL_BRUTAL,    loaded.xpRollBrutal);
    }

    // ---------------------------------------------------------------------
    // Validation: missing section
    // ---------------------------------------------------------------------

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void missingSectionRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        // Valid file with xp-roll-brutal section deliberately omitted.
        String body =
                "all-lines:\n  - \"a\"\n" +
                "all-lines-with-xp:\n  - \"a\"\n" +
                "all-lines-no-xp:\n  - \"a\"\n" +
                "same-dim-lines:\n  - \"%s\"\n" +
                "diff-dim-lines:\n  - \"a\"\n" +
                "xp-roll-lucky:\n  - \"a\"\n" +
                "xp-roll-mid:\n  - \"a\"\n";
                // no xp-roll-brutal:
        write(f, body);
        DeathMessageStore.fromFile(f);
    }

    // ---------------------------------------------------------------------
    // Validation: empty pool
    // ---------------------------------------------------------------------

    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void emptySectionRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        // xp-roll-mid header present but no list items under it.
        String body =
                "all-lines:\n  - \"a\"\n" +
                "all-lines-with-xp:\n  - \"a\"\n" +
                "all-lines-no-xp:\n  - \"a\"\n" +
                "same-dim-lines:\n  - \"%s\"\n" +
                "diff-dim-lines:\n  - \"a\"\n" +
                "xp-roll-lucky:\n  - \"a\"\n" +
                "xp-roll-mid:\n" +                       // header only, no items
                "xp-roll-brutal:\n  - \"a\"\n";
        write(f, body);
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
    // Parser fixes from v2.2 code review (B1, B4, S4)
    // ---------------------------------------------------------------------

    /** B1: a '#' inside a quoted string must not be treated as a comment. */
    @Test public void hashInsideQuotedMessageSurvives() throws Exception {
        File f = freshFile("death-msgs.yml");
        String body =
                "all-lines:\n  - \"hashtag #yolo\"\n" +
                "all-lines-with-xp:\n  - \"a\"\n" +
                "all-lines-no-xp:\n  - \"a\"\n" +
                "same-dim-lines:\n  - \"%s\"\n" +
                "diff-dim-lines:\n  - \"a\"\n" +
                "xp-roll-lucky:\n  - \"a\"\n" +
                "xp-roll-mid:\n  - \"a\"\n" +
                "xp-roll-brutal:\n  - \"a\"\n";
        write(f, body);
        DeathMessageStore d = DeathMessageStore.fromFile(f);
        assertEquals("hashtag #yolo", d.allLines[0]);
    }

    /** B4: a line that's indented but isn't a list item is a hard error. */
    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void garbageLineInSectionRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        String body =
                "all-lines:\n  - \"a\"\n  oops not a list item\n" +
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

    /** B4: a list item before any section header is a hard error. */
    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void leadingListItemRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        String body =
                "  - \"orphan\"\n" +
                "all-lines:\n  - \"a\"\n";
        write(f, body);
        DeathMessageStore.fromFile(f);
    }

    /** S4: blank string line in a pool is rejected. */
    @Test(expected = DeathMessageStore.InvalidDeathMessageFile.class)
    public void blankMessageLineRejected() throws Exception {
        File f = freshFile("death-msgs.yml");
        String body =
                "all-lines:\n  - \"\"\n" +
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
}
