package com.flashminat0.selectivekeepinv;

/**
 * Per-player preservation mode.
 *
 * <ul>
 *   <li>{@link #DEFAULT}: XP-cost preservation. What gets kept is a function of
 *   the player's experience level at the moment of death. XP itself drops as
 *   orbs (vanilla behavior). See {@link PreservationPlan#resolveDefault(int, int, Config)}.</li>
 *
 *   <li>{@link #ALL}: keep everything (full inventory, armor, offhand,
 *   accessories from Baubles + Trinkets, and full XP level). No cost.</li>
 * </ul>
 *
 * The name() values are user-facing (command args and stored on disk), so they
 * must remain stable.
 */
public enum Mode {
    DEFAULT("default"),
    ALL("all");

    private final String name;

    Mode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** Case-insensitive lookup. Returns null if no match. */
    public static Mode fromName(String input) {
        if (input == null) return null;
        for (Mode m : values()) {
            if (m.name.equalsIgnoreCase(input)) return m;
        }
        return null;
    }
}
