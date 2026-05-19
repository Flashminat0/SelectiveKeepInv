package com.flashminat0.selectivekeepinv;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pure-math tests for {@link EventHandler#selectXpRollPool}. Verifies that
 * a divisor maps to the right pool given the configured [min, max] range.
 *
 * <p>Uses {@link DeathMessageStore#defaults()} so each pool array is a stable
 * static reference, letting us {@link org.junit.Assert#assertSame} on the
 * return value instead of comparing contents.
 */
public class XpRollFlavorTest {

    private static final DeathMessageStore MSGS = DeathMessageStore.defaults();

    private static Config defaults() { return Config.defaults(); }

    private static Config range(int min, int max) {
        Config c = Config.defaults();
        c.divisorMin = min;
        c.divisorMax = max;
        return c;
    }

    // ---------------------------------------------------------------------
    // Default range [1, 3] keeps the legacy 1=lucky / 2=mid / 3=brutal mapping
    // ---------------------------------------------------------------------

    @Test public void defaultRangeLegacyMapping() {
        Config cfg = defaults();
        assertSame(MSGS.xpRollLucky,  EventHandler.selectXpRollPool(1, cfg, MSGS));
        assertSame(MSGS.xpRollMid,    EventHandler.selectXpRollPool(2, cfg, MSGS));
        assertSame(MSGS.xpRollBrutal, EventHandler.selectXpRollPool(3, cfg, MSGS));
    }

    // ---------------------------------------------------------------------
    // Widened range [1, 10]: thirds get bucketed cleanly
    // ---------------------------------------------------------------------

    @Test public void widenedRangeBucketsByThirds() {
        Config cfg = range(1, 10);
        // quality = (10 - divisor) / 9
        // divisor 1..4 -> quality 1.0..0.67  -> lucky
        // divisor 5..7 -> quality 0.56..0.33 -> mid
        // divisor 8..10 -> quality 0.22..0.0 -> brutal
        for (int d = 1; d <= 4; d++)  assertSame("d=" + d, MSGS.xpRollLucky,  EventHandler.selectXpRollPool(d, cfg, MSGS));
        for (int d = 5; d <= 7; d++)  assertSame("d=" + d, MSGS.xpRollMid,    EventHandler.selectXpRollPool(d, cfg, MSGS));
        for (int d = 8; d <= 10; d++) assertSame("d=" + d, MSGS.xpRollBrutal, EventHandler.selectXpRollPool(d, cfg, MSGS));
    }

    // ---------------------------------------------------------------------
    // Non-1 min: [2, 4] should still produce all three buckets
    // ---------------------------------------------------------------------

    @Test public void rangeStartingAboveOneStillBuckets() {
        Config cfg = range(2, 4);
        assertSame(MSGS.xpRollLucky,  EventHandler.selectXpRollPool(2, cfg, MSGS));
        assertSame(MSGS.xpRollMid,    EventHandler.selectXpRollPool(3, cfg, MSGS));
        assertSame(MSGS.xpRollBrutal, EventHandler.selectXpRollPool(4, cfg, MSGS));
    }

    @Test public void wideRangeStartingAboveOne() {
        Config cfg = range(5, 15);
        // quality = (15 - d) / 10
        // lucky: q >= 2/3 (~0.667) → d in 5..8
        // mid:   q >= 1/3 (~0.333) → d in 9..11
        // brutal: else              → d in 12..15
        assertSame(MSGS.xpRollLucky,  EventHandler.selectXpRollPool(5, cfg, MSGS));   // 1.0
        assertSame(MSGS.xpRollLucky,  EventHandler.selectXpRollPool(8, cfg, MSGS));   // 0.7
        assertSame(MSGS.xpRollMid,    EventHandler.selectXpRollPool(9, cfg, MSGS));   // 0.6
        assertSame(MSGS.xpRollMid,    EventHandler.selectXpRollPool(11, cfg, MSGS));  // 0.4
        assertSame(MSGS.xpRollBrutal, EventHandler.selectXpRollPool(12, cfg, MSGS));  // 0.3
        assertSame(MSGS.xpRollBrutal, EventHandler.selectXpRollPool(13, cfg, MSGS));  // 0.2
        assertSame(MSGS.xpRollBrutal, EventHandler.selectXpRollPool(15, cfg, MSGS));  // 0.0
    }

    // ---------------------------------------------------------------------
    // min == max: no gamble -> no flavor line
    // ---------------------------------------------------------------------

    @Test public void minEqualsMaxReturnsNull() {
        assertNull(EventHandler.selectXpRollPool(1, range(1, 1), MSGS));
        assertNull(EventHandler.selectXpRollPool(5, range(5, 5), MSGS));
    }

    // ---------------------------------------------------------------------
    // Defensive clamping
    // ---------------------------------------------------------------------

    @Test public void zeroOrNegativeMinIsClampedToOne() {
        // cfg says [0, 3] -> behaves like [1, 3]
        Config cfg = range(0, 3);
        assertSame(MSGS.xpRollLucky,  EventHandler.selectXpRollPool(1, cfg, MSGS));
        assertSame(MSGS.xpRollMid,    EventHandler.selectXpRollPool(2, cfg, MSGS));
        assertSame(MSGS.xpRollBrutal, EventHandler.selectXpRollPool(3, cfg, MSGS));
    }

    @Test public void maxLessThanMinClampsToMinAndReturnsNull() {
        // cfg says [3, 1] -> max bumped up to min, so effectively [3, 3]
        Config cfg = range(3, 1);
        assertNull(EventHandler.selectXpRollPool(3, cfg, MSGS));
    }

    // ---------------------------------------------------------------------
    // Boundary sanity: best roll is always lucky, worst is always brutal
    // ---------------------------------------------------------------------

    @Test public void minimumDivisorAlwaysLucky() {
        for (int max : new int[]{3, 5, 10, 100}) {
            Config cfg = range(1, max);
            assertSame("max=" + max, MSGS.xpRollLucky,
                    EventHandler.selectXpRollPool(1, cfg, MSGS));
        }
    }

    @Test public void maximumDivisorAlwaysBrutal() {
        for (int max : new int[]{3, 5, 10, 100}) {
            Config cfg = range(1, max);
            assertSame("max=" + max, MSGS.xpRollBrutal,
                    EventHandler.selectXpRollPool(max, cfg, MSGS));
        }
    }
}
