package com.flashminat0.selectivekeepinv;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable description of what to preserve on a single death.
 *
 * <p>Two factory methods produce a plan:
 * <ul>
 *   <li>{@link #resolveDefault(int, int)} for {@link Mode#DEFAULT}: applies the
 *       XP-cost threshold table below.</li>
 *   <li>{@link #all(int)} for {@link Mode#ALL}: preserves everything.</li>
 * </ul>
 *
 * <p>Default-mode threshold table (cumulative; each entry unlocks at that
 * level and adds to everything below it):
 *
 * <pre>
 *   Level  1..9  hotbar slots 1..N (leftmost N)
 *   Level    10  + offhand
 *   Level    11  + helmet
 *   Level    16  + chestplate
 *   Level    21  + leggings
 *   Level    26  + boots (all armor done)
 *   Level    50  + accessories (Baubles AND Trinkets)
 *   Level   100  + main inventory (27 slots above hotbar)
 *                + XP carryover: respawn with (level - 100) / D, where
 *                  D is a per-death random divisor in {1, 2, 3}. The
 *                  caller (death handler) rolls D and passes it in.
 * </pre>
 *
 * Below level 100 in default mode, XP on respawn is 0 (cost paid in XP).
 * At level 100+, the carryover is a gamble: you might get back nearly
 * everything (D=1), half (D=2), or only a third (D=3).
 * In all mode, XP is fully retained, no divisor, no roll.
 */
public final class PreservationPlan {

    /** Count of hotbar slots preserved from the left (0..9). */
    public final int hotbarSlots;
    public final boolean offhand;
    public final boolean helmet;
    public final boolean chestplate;
    public final boolean leggings;
    public final boolean boots;
    /** Combined Baubles + Trinkets accessory slots. */
    public final boolean accessories;
    /** The 27 main inventory slots above the hotbar. */
    public final boolean mainInventory;
    /** XP level the player respawns at. */
    public final int xpRetained;

    private PreservationPlan(int hotbarSlots, boolean offhand,
                             boolean helmet, boolean chestplate, boolean leggings, boolean boots,
                             boolean accessories, boolean mainInventory, int xpRetained) {
        this.hotbarSlots   = hotbarSlots;
        this.offhand       = offhand;
        this.helmet        = helmet;
        this.chestplate    = chestplate;
        this.leggings      = leggings;
        this.boots         = boots;
        this.accessories   = accessories;
        this.mainInventory = mainInventory;
        this.xpRetained    = xpRetained;
    }

    // ---------------------------------------------------------------------
    // Factories
    // ---------------------------------------------------------------------

    /** All-zero plan: preserve nothing, no XP retained. */
    public static PreservationPlan empty() {
        return new PreservationPlan(0, false, false, false, false, false, false, false, 0);
    }

    /**
     * Default-mode plan derived from the death XP level using the threshold table
     * above. Negative xpLevel clamps to 0. The xpDivisor controls the XP
     * carryover rate at level &gt;= 100; the caller rolls a random value in
     * {1, 2, 3} per death. Divisors &lt; 1 are clamped to 1.
     */
    public static PreservationPlan resolveDefault(int xpLevel, int xpDivisor) {
        if (xpLevel <= 0) return empty();
        if (xpDivisor < 1) xpDivisor = 1;

        int hotbar      = Math.min(xpLevel, 9);
        boolean offh    = xpLevel >= 10;
        boolean helmet  = xpLevel >= 11;
        boolean chest   = xpLevel >= 16;
        boolean legs    = xpLevel >= 21;
        boolean boots   = xpLevel >= 26;
        boolean accs    = xpLevel >= 50;
        boolean mainInv = xpLevel >= 100;
        int retained    = xpLevel >= 100 ? Math.max(0, (xpLevel - 100) / xpDivisor) : 0;

        return new PreservationPlan(hotbar, offh, helmet, chest, legs, boots, accs, mainInv, retained);
    }

    /**
     * All-mode plan: preserve everything regardless of XP cost. XP retained is
     * the death level (no cost).
     */
    public static PreservationPlan all(int xpLevelAtDeath) {
        return new PreservationPlan(
                9, true, true, true, true, true, true, true,
                Math.max(0, xpLevelAtDeath));
    }

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    /** True if nothing at all is preserved (player gets default vanilla death). */
    public boolean isEmpty() {
        return hotbarSlots == 0 && !offhand && !helmet && !chestplate
                && !leggings && !boots && !accessories && !mainInventory
                && xpRetained == 0;
    }

    /** True if every armor slot is preserved. */
    public boolean fullArmor() {
        return helmet && chestplate && leggings && boots;
    }

    /** True if both vanilla inventory regions (hotbar all 9 + main 27) are preserved. */
    public boolean fullInventory() {
        return hotbarSlots == 9 && mainInventory;
    }

    /**
     * Human-readable list of preserved components, in display order.
     * Empty list if {@link #isEmpty()} returns true.
     */
    public List<String> describeParts() {
        List<String> parts = new ArrayList<>();

        if (fullInventory() && offhand && fullArmor() && accessories) {
            // Compact "everything" wording.
            parts.add("full inventory");
            parts.add("offhand");
            parts.add("full armor");
            parts.add("accessories");
        } else {
            if (hotbarSlots > 0) {
                parts.add(hotbarSlots == 1 ? "1 hotbar slot" : hotbarSlots + " hotbar slots");
            }
            if (offhand)    parts.add("offhand");
            if (fullArmor()) {
                parts.add("full armor");
            } else {
                if (helmet)     parts.add("helmet");
                if (chestplate) parts.add("chestplate");
                if (leggings)   parts.add("leggings");
                if (boots)      parts.add("boots");
            }
            if (accessories) parts.add("accessories");
            if (mainInventory) parts.add("main inventory");
        }

        if (xpRetained > 0) {
            parts.add(xpRetained + " XP retained");
        }
        return parts;
    }
}
