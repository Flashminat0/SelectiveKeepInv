package com.flashminat0.selectivekeepinv;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Covers the XP-cost threshold table that drives {@link Mode#DEFAULT}, plus
 * the simpler {@link Mode#ALL} plan and the description renderer.
 *
 * <p>Most tests pass {@code xpDivisor = 1} to {@link
 * PreservationPlan#resolveDefault(int, int)} for predictability; the divisor
 * only affects XP carryover at level >= 100. A dedicated section below tests
 * each divisor value (1, 2, 3) and the input clamping.
 */
public class PreservationPlanTest {

    /** Convenience: most threshold tests don't care about the divisor. */
    private static PreservationPlan resolve(int xpLevel) {
        return PreservationPlan.resolveDefault(xpLevel, 1);
    }

    // ---------------------------------------------------------------------
    // Empty / negative
    // ---------------------------------------------------------------------

    @Test public void emptyIsAllFalseZero() {
        PreservationPlan p = PreservationPlan.empty();
        assertTrue(p.isEmpty());
        assertEquals(0, p.hotbarSlots);
        assertEquals(0, p.xpRetained);
        assertTrue(p.describeParts().isEmpty());
    }

    @Test public void resolveDefaultLevelZeroIsEmpty() {
        assertTrue(resolve(0).isEmpty());
    }

    @Test public void resolveDefaultNegativeIsEmpty() {
        assertTrue(resolve(-50).isEmpty());
    }

    // ---------------------------------------------------------------------
    // Hotbar thresholds (1..9)
    // ---------------------------------------------------------------------

    @Test public void level1Keeps1HotbarSlot() {
        PreservationPlan p = resolve(1);
        assertEquals(1, p.hotbarSlots);
        assertFalse(p.offhand);
        assertFalse(p.helmet);
        assertFalse(p.mainInventory);
        assertEquals(0, p.xpRetained);
    }

    @Test public void level5Keeps5HotbarSlots() {
        PreservationPlan p = resolve(5);
        assertEquals(5, p.hotbarSlots);
        assertFalse(p.offhand);
    }

    @Test public void level9KeepsAllHotbarSlots() {
        assertEquals(9, resolve(9).hotbarSlots);
    }

    @Test public void hotbarCapsAt9() {
        assertEquals(9, resolve(100).hotbarSlots);
        assertEquals(9, resolve(9999).hotbarSlots);
    }

    // ---------------------------------------------------------------------
    // Offhand at 10
    // ---------------------------------------------------------------------

    @Test public void level9DoesNotIncludeOffhand() {
        assertFalse(resolve(9).offhand);
    }

    @Test public void level10IncludesOffhand() {
        PreservationPlan p = resolve(10);
        assertEquals(9, p.hotbarSlots);
        assertTrue(p.offhand);
        assertFalse(p.helmet);
    }

    // ---------------------------------------------------------------------
    // Armor unlocks: helmet 11, chestplate 16, leggings 21, boots 26
    // ---------------------------------------------------------------------

    @Test public void helmetUnlocksAt11() {
        assertFalse(resolve(10).helmet);
        assertTrue (resolve(11).helmet);
        assertFalse(resolve(11).chestplate);
    }

    @Test public void chestplateUnlocksAt16() {
        assertFalse(resolve(15).chestplate);
        assertTrue (resolve(16).chestplate);
        assertFalse(resolve(16).leggings);
    }

    @Test public void leggingsUnlockAt21() {
        assertFalse(resolve(20).leggings);
        assertTrue (resolve(21).leggings);
        assertFalse(resolve(21).boots);
    }

    @Test public void bootsUnlockAt26() {
        assertFalse(resolve(25).boots);
        assertTrue (resolve(26).boots);
        assertTrue (resolve(26).fullArmor());
    }

    @Test public void level25MatchesLevel21Armor() {
        PreservationPlan a = resolve(21);
        PreservationPlan b = resolve(25);
        assertEquals(a.helmet,     b.helmet);
        assertEquals(a.chestplate, b.chestplate);
        assertEquals(a.leggings,   b.leggings);
        assertEquals(a.boots,      b.boots);
    }

    // ---------------------------------------------------------------------
    // Accessories at 50
    // ---------------------------------------------------------------------

    @Test public void accessoriesUnlockAt50() {
        assertFalse(resolve(49).accessories);
        assertTrue (resolve(50).accessories);
        assertFalse(resolve(50).mainInventory);
    }

    @Test public void level83SameAsLevel50ForItems() {
        PreservationPlan a = resolve(50);
        PreservationPlan b = resolve(83);
        assertEquals(a.hotbarSlots,   b.hotbarSlots);
        assertEquals(a.offhand,       b.offhand);
        assertEquals(a.fullArmor(),   b.fullArmor());
        assertEquals(a.accessories,   b.accessories);
        assertEquals(a.mainInventory, b.mainInventory);
    }

    // ---------------------------------------------------------------------
    // Main inventory + XP carryover at 100
    // ---------------------------------------------------------------------

    @Test public void mainInventoryUnlocksAt100() {
        assertFalse(resolve(99).mainInventory);
        assertTrue (resolve(100).mainInventory);
        assertTrue (resolve(100).fullInventory());
    }

    @Test public void xpRetainedZeroBelow100() {
        assertEquals(0, resolve(50).xpRetained);
        assertEquals(0, resolve(99).xpRetained);
    }

    @Test public void xpRetainedZeroAt100Exactly() {
        // level 100 - 100 = 0, regardless of divisor
        assertEquals(0, PreservationPlan.resolveDefault(100, 1).xpRetained);
        assertEquals(0, PreservationPlan.resolveDefault(100, 2).xpRetained);
        assertEquals(0, PreservationPlan.resolveDefault(100, 3).xpRetained);
    }

    // ---------------------------------------------------------------------
    // XP carryover divisor: D=1 (best case)
    // ---------------------------------------------------------------------

    @Test public void divisor1KeepsAllLevelMinus100() {
        assertEquals(50,  PreservationPlan.resolveDefault(150, 1).xpRetained);
        assertEquals(100, PreservationPlan.resolveDefault(200, 1).xpRetained);
        assertEquals(900, PreservationPlan.resolveDefault(1000, 1).xpRetained);
    }

    // ---------------------------------------------------------------------
    // XP carryover divisor: D=2 (half)
    // ---------------------------------------------------------------------

    @Test public void divisor2KeepsHalfLevelMinus100() {
        assertEquals(25,  PreservationPlan.resolveDefault(150, 2).xpRetained);
        assertEquals(50,  PreservationPlan.resolveDefault(200, 2).xpRetained);
        assertEquals(450, PreservationPlan.resolveDefault(1000, 2).xpRetained);
    }

    // ---------------------------------------------------------------------
    // XP carryover divisor: D=3 (third, worst case)
    // ---------------------------------------------------------------------

    @Test public void divisor3KeepsThirdLevelMinus100() {
        // integer division: (150-100)/3 = 16
        assertEquals(16,  PreservationPlan.resolveDefault(150, 3).xpRetained);
        assertEquals(33,  PreservationPlan.resolveDefault(200, 3).xpRetained);
        assertEquals(300, PreservationPlan.resolveDefault(1000, 3).xpRetained);
    }

    @Test public void divisorBelow1IsClampedTo1() {
        // (200 - 100) / 1 = 100, no matter what nonsense gets passed
        assertEquals(100, PreservationPlan.resolveDefault(200, 0).xpRetained);
        assertEquals(100, PreservationPlan.resolveDefault(200, -5).xpRetained);
    }

    @Test public void divisorIgnoredBelowThreshold() {
        // Below level 100, divisor doesn't matter (retained is always 0).
        assertEquals(0, PreservationPlan.resolveDefault(50, 1).xpRetained);
        assertEquals(0, PreservationPlan.resolveDefault(50, 2).xpRetained);
        assertEquals(0, PreservationPlan.resolveDefault(50, 3).xpRetained);
    }

    // ---------------------------------------------------------------------
    // ALL mode (no divisor, full retention)
    // ---------------------------------------------------------------------

    @Test public void allModeKeepsEverythingIncludingXp() {
        PreservationPlan p = PreservationPlan.all(42);
        assertEquals(9, p.hotbarSlots);
        assertTrue(p.offhand);
        assertTrue(p.fullArmor());
        assertTrue(p.accessories);
        assertTrue(p.mainInventory);
        assertEquals(42, p.xpRetained);
        assertFalse(p.isEmpty());
    }

    @Test public void allModeWithZeroLevelStillKeepsItems() {
        PreservationPlan p = PreservationPlan.all(0);
        assertTrue(p.fullInventory());
        assertEquals(0, p.xpRetained);
    }

    @Test public void allModeNegativeLevelClampsXpToZero() {
        assertEquals(0, PreservationPlan.all(-5).xpRetained);
    }

    // ---------------------------------------------------------------------
    // describeParts: spot-check formatting
    // ---------------------------------------------------------------------

    @Test public void describeLevel1IsSingularHotbarSlot() {
        assertEquals("1 hotbar slot", resolve(1).describeParts().get(0));
    }

    @Test public void describeLevel5IsPlural() {
        assertEquals("5 hotbar slots", resolve(5).describeParts().get(0));
    }

    @Test public void describeLevel20HasPartialArmor() {
        // level 20: hotbar (9) + offhand + helmet + chestplate
        java.util.List<String> p = resolve(20).describeParts();
        assertTrue(p.contains("9 hotbar slots"));
        assertTrue(p.contains("offhand"));
        assertTrue(p.contains("helmet"));
        assertTrue(p.contains("chestplate"));
        assertFalse(p.contains("leggings"));
        assertFalse(p.contains("full armor"));
    }

    @Test public void describeLevel26CompactsToFullArmor() {
        java.util.List<String> p = resolve(26).describeParts();
        assertTrue(p.contains("full armor"));
        assertFalse(p.contains("boots"));
    }

    @Test public void describeLevel150D1IncludesXpRetained() {
        java.util.List<String> p = PreservationPlan.resolveDefault(150, 1).describeParts();
        assertTrue(p.contains("50 XP retained"));
    }

    @Test public void describeAllModeAt100() {
        java.util.List<String> p = PreservationPlan.all(100).describeParts();
        assertTrue(p.contains("full inventory"));
        assertTrue(p.contains("full armor"));
        assertTrue(p.contains("accessories"));
        assertTrue(p.contains("100 XP retained"));
    }
}
