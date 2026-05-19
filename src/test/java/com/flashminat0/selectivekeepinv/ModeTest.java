package com.flashminat0.selectivekeepinv;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Mode is small but its {@code name()} values are user-facing (commands)
 * AND on-disk (players.json), so they have to round-trip exactly.
 */
public class ModeTest {

    @Test public void defaultGetNameIsLowercase() {
        assertEquals("default", Mode.DEFAULT.getName());
    }

    @Test public void allGetNameIsLowercase() {
        assertEquals("all", Mode.ALL.getName());
    }

    @Test public void fromNameRoundTripsExact() {
        for (Mode m : Mode.values()) {
            assertEquals(m, Mode.fromName(m.getName()));
        }
    }

    @Test public void fromNameIsCaseInsensitive() {
        assertEquals(Mode.DEFAULT, Mode.fromName("DEFAULT"));
        assertEquals(Mode.DEFAULT, Mode.fromName("Default"));
        assertEquals(Mode.DEFAULT, Mode.fromName("dEfAuLt"));
        assertEquals(Mode.ALL,     Mode.fromName("ALL"));
        assertEquals(Mode.ALL,     Mode.fromName("All"));
    }

    @Test public void fromNameNullIsNull() {
        assertNull(Mode.fromName(null));
    }

    @Test public void fromNameUnknownIsNull() {
        assertNull(Mode.fromName(""));
        assertNull(Mode.fromName("nope"));
        assertNull(Mode.fromName("partial"));
        assertNull(Mode.fromName("default ")); // trailing space; not trimmed
    }
}
