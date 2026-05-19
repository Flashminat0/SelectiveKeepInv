package com.flashminat0.selectivekeepinv;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PlayerList}.
 *
 * Each test points {@link SelectiveKeepInv#configDir} at a fresh temp folder
 * and resets the static state so tests are fully isolated.
 */
public class PlayerListTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        SelectiveKeepInv.configDir = tempFolder.newFolder("selectivekeepinv");
        clearInternalState();
    }

    @After
    public void tearDown() throws Exception {
        clearInternalState();
    }

    private void clearInternalState() throws Exception {
        Field f = PlayerList.class.getDeclaredField("players");
        f.setAccessible(true);
        f.set(null, new HashMap<UUID, Mode>());
    }

    // ---------------------------------------------------------------------
    // set / remove / contains / getMode
    // ---------------------------------------------------------------------

    @Test
    public void setNewPlayerReturnsAdded() {
        assertEquals(PlayerList.SetResult.ADDED,
                PlayerList.set(UUID.randomUUID(), Mode.DEFAULT));
    }

    @Test
    public void setSameModeReturnsUnchanged() {
        UUID u = UUID.randomUUID();
        PlayerList.set(u, Mode.DEFAULT);
        assertEquals(PlayerList.SetResult.UNCHANGED,
                PlayerList.set(u, Mode.DEFAULT));
    }

    @Test
    public void setDifferentModeReturnsSwitched() {
        UUID u = UUID.randomUUID();
        PlayerList.set(u, Mode.DEFAULT);
        assertEquals(PlayerList.SetResult.SWITCHED,
                PlayerList.set(u, Mode.ALL));
        assertEquals(Mode.ALL, PlayerList.getMode(u));
    }

    @Test
    public void containsTrueAfterSet() {
        UUID u = UUID.randomUUID();
        assertFalse(PlayerList.contains(u));
        PlayerList.set(u, Mode.DEFAULT);
        assertTrue(PlayerList.contains(u));
    }

    @Test
    public void removeReturnsTrueWhenPresent() {
        UUID u = UUID.randomUUID();
        PlayerList.set(u, Mode.ALL);
        assertTrue(PlayerList.remove(u));
        assertFalse(PlayerList.contains(u));
        assertNull(PlayerList.getMode(u));
    }

    @Test
    public void removeReturnsFalseWhenAbsent() {
        assertFalse(PlayerList.remove(UUID.randomUUID()));
    }

    @Test
    public void getModeNullForUnknownPlayer() {
        assertNull(PlayerList.getMode(UUID.randomUUID()));
    }

    @Test
    public void allReturnsDefensiveCopy() {
        UUID u = UUID.randomUUID();
        PlayerList.set(u, Mode.DEFAULT);
        Map<UUID, Mode> snapshot = PlayerList.all();
        snapshot.clear();
        assertTrue(PlayerList.contains(u));
        assertEquals(1, PlayerList.all().size());
    }

    @Test
    public void multiplePlayersCoexistWithDifferentModes() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        PlayerList.set(u1, Mode.DEFAULT);
        PlayerList.set(u2, Mode.ALL);
        assertEquals(Mode.DEFAULT, PlayerList.getMode(u1));
        assertEquals(Mode.ALL,     PlayerList.getMode(u2));
        assertEquals(2, PlayerList.all().size());
    }

    // ---------------------------------------------------------------------
    // persistence
    // ---------------------------------------------------------------------

    @Test
    public void setWritesPlayersJsonToConfigDir() {
        PlayerList.set(UUID.randomUUID(), Mode.DEFAULT);
        File f = new File(SelectiveKeepInv.configDir, "players.json");
        assertTrue("players.json should exist after set()", f.exists());
        assertTrue("players.json should be non-empty", f.length() > 0);
    }

    @Test
    public void removeUpdatesPersistedFile() throws Exception {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        PlayerList.set(u1, Mode.DEFAULT);
        PlayerList.set(u2, Mode.ALL);
        PlayerList.remove(u1);

        clearInternalState();
        PlayerList.load();

        assertNull(PlayerList.getMode(u1));
        assertEquals(Mode.ALL, PlayerList.getMode(u2));
    }

    @Test
    public void loadRestoresModesFromDisk() throws Exception {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        PlayerList.set(u1, Mode.DEFAULT);
        PlayerList.set(u2, Mode.ALL);

        clearInternalState();
        assertFalse(PlayerList.contains(u1));
        assertFalse(PlayerList.contains(u2));

        PlayerList.load();
        assertEquals(Mode.DEFAULT, PlayerList.getMode(u1));
        assertEquals(Mode.ALL,     PlayerList.getMode(u2));
    }

    @Test
    public void loadWithNoFileLeavesStateUntouched() {
        File f = new File(SelectiveKeepInv.configDir, "players.json");
        assertFalse(f.exists());
        PlayerList.load();
        assertTrue(PlayerList.all().isEmpty());
    }

    @Test
    public void loadWithCorruptedFileFailsGracefully() throws Exception {
        File f = new File(SelectiveKeepInv.configDir, "players.json");
        try (FileWriter w = new FileWriter(f)) {
            w.write("{ not valid json [[[");
        }
        PlayerList.load();
        assertTrue(PlayerList.all().isEmpty());
    }

    @Test
    public void loadWithEmptyObjectProducesEmptyState() throws Exception {
        File f = new File(SelectiveKeepInv.configDir, "players.json");
        try (FileWriter w = new FileWriter(f)) {
            w.write("{}");
        }
        PlayerList.load();
        assertTrue(PlayerList.all().isEmpty());
    }

    @Test
    public void loadIgnoresUnknownModeStrings() throws Exception {
        UUID u = UUID.randomUUID();
        File f = new File(SelectiveKeepInv.configDir, "players.json");
        try (FileWriter w = new FileWriter(f)) {
            w.write("{\"" + u + "\":\"nonsense_mode\"}");
        }
        PlayerList.load();
        assertFalse("Unknown mode strings should be dropped", PlayerList.contains(u));
    }

    @Test
    public void switchModeSurvivesReload() throws Exception {
        UUID u = UUID.randomUUID();
        PlayerList.set(u, Mode.DEFAULT);
        PlayerList.set(u, Mode.ALL);

        clearInternalState();
        PlayerList.load();

        assertEquals(Mode.ALL, PlayerList.getMode(u));
    }

    @Test
    public void repeatedSetSameModeDoesNotDuplicate() throws Exception {
        UUID u = UUID.randomUUID();
        PlayerList.set(u, Mode.DEFAULT);
        PlayerList.set(u, Mode.DEFAULT);
        PlayerList.set(u, Mode.DEFAULT);

        clearInternalState();
        PlayerList.load();

        assertEquals(Mode.DEFAULT, PlayerList.getMode(u));
        assertEquals(1, PlayerList.all().size());
    }
}
