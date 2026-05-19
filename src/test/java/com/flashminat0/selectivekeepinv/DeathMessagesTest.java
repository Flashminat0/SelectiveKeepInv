package com.flashminat0.selectivekeepinv;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Cheap structural checks on the message pools to protect against editing
 * mistakes (empty array, null entry, missing/extra %s placeholder).
 */
public class DeathMessagesTest {

    // ---------------------------------------------------------------------
    // Pools must be non-empty and contain no null/blank entries
    // ---------------------------------------------------------------------

    @Test public void allLinesNonEmptyAndClean() { assertCleanPool(DeathMessages.ALL_LINES); }
    @Test public void allLinesWithXpClean()      { assertCleanPool(DeathMessages.ALL_LINES_WITH_XP); }
    @Test public void allLinesNoXpClean()        { assertCleanPool(DeathMessages.ALL_LINES_NO_XP); }
    @Test public void sameDimLinesClean()        { assertCleanPool(DeathMessages.SAME_DIM_LINES); }
    @Test public void diffDimLinesClean()        { assertCleanPool(DeathMessages.DIFF_DIM_LINES); }
    @Test public void xpRollLuckyClean()         { assertCleanPool(DeathMessages.XP_ROLL_LUCKY); }
    @Test public void xpRollMidClean()           { assertCleanPool(DeathMessages.XP_ROLL_MID); }
    @Test public void xpRollBrutalClean()        { assertCleanPool(DeathMessages.XP_ROLL_BRUTAL); }

    // ---------------------------------------------------------------------
    // SAME_DIM lines are formatted with String.format(line, distance), so
    // each one MUST contain exactly one %s and no other format specifiers.
    // Anything else would either ArrayFormatException at runtime or print
    // the distance in the wrong slot.
    // ---------------------------------------------------------------------

    @Test public void sameDimLinesHaveExactlyOnePercentS() {
        for (String line : DeathMessages.SAME_DIM_LINES) {
            int sCount = countOccurrences(line, "%s");
            int percentCount = countPercentSpecifiers(line);
            assertEquals("Line must have exactly one %s placeholder: " + line, 1, sCount);
            assertEquals("Line must have no other format specifiers: " + line, 1, percentCount);
        }
    }

    // ---------------------------------------------------------------------
    // No %s in pools that aren't formatted (would just print the literal %s).
    // ---------------------------------------------------------------------

    @Test public void unformattedPoolsHaveNoPercentS() {
        assertNoPercentS(DeathMessages.ALL_LINES);
        assertNoPercentS(DeathMessages.ALL_LINES_WITH_XP);
        assertNoPercentS(DeathMessages.ALL_LINES_NO_XP);
        assertNoPercentS(DeathMessages.DIFF_DIM_LINES);
        assertNoPercentS(DeathMessages.XP_ROLL_LUCKY);
        assertNoPercentS(DeathMessages.XP_ROLL_MID);
        assertNoPercentS(DeathMessages.XP_ROLL_BRUTAL);
    }

    // ---------------------------------------------------------------------
    // String.format on every SAME_DIM line should succeed with one arg
    // (acts as a fuzz check for stray % characters)
    // ---------------------------------------------------------------------

    @Test public void everySameDimLineFormatsWithoutError() {
        for (String line : DeathMessages.SAME_DIM_LINES) {
            String formatted = String.format(line, "42");
            assertTrue("Formatted line should contain the substituted value: " + line,
                    formatted.contains("42"));
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static void assertCleanPool(String[] pool) {
        assertNotNull(pool);
        assertTrue("Pool must not be empty", pool.length > 0);
        for (int i = 0; i < pool.length; i++) {
            assertNotNull("Pool entry [" + i + "] is null", pool[i]);
            assertFalse ("Pool entry [" + i + "] is blank", pool[i].trim().isEmpty());
        }
    }

    private static void assertNoPercentS(String[] pool) {
        for (String line : pool) {
            assertFalse("Unformatted pool must not contain %s: " + line, line.contains("%s"));
        }
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /** Counts any printf format specifier: `%` followed by anything but `%`. */
    private static int countPercentSpecifiers(String s) {
        int count = 0;
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '%' && s.charAt(i + 1) != '%') count++;
        }
        return count;
    }
}
