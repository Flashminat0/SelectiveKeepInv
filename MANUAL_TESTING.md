# Manual Test Plan

`EventHandler`, `CommandKeepInv`, and the two `*Compat` classes touch
Minecraft runtime machinery (NBT, entities, capabilities, the event bus) that
isn't worth unit-testing. Run through this checklist on a dev server before
shipping any change.

Launch a dev server with the mod loaded:

```
./gradlew runServer
```

Use a second account or a second client to test as a non-op player where
needed.

## A. Command behavior

| #   | Step | Expected |
|-----|------|----------|
| A1  | As op, `/keepinv` with no args | Usage message |
| A2  | As op, `/keepinv list` on a fresh install | "No players currently have keepInventory enabled." |
| A3  | `/keepinv add Player1` (player has joined) | "Added Player1 in default mode." |
| A4  | `/keepinv list` again | Player1 appears with mode `default` |
| A5  | `/keepinv add Player1` again | "Player1 is already on the list in default mode." |
| A6  | `/keepinv add Player1 all` | "Switched Player1 from default to all mode." |
| A7  | `/keepinv add Player1 all` again | "Player1 is already on the list in all mode." |
| A8  | `/keepinv add NeverJoinedPlayer` | "Unknown player: NeverJoinedPlayer (they need to have joined the server at least once)." |
| A9  | `/keepinv add Player1 garbage` | Usage error message |
| A10 | `/keepinv list Player1` | "Player1: all" |
| A11 | `/keepinv list Player1 extra` | Usage error message (S4 strict validation) |
| A12 | `/keepinv remove Player1` | "Removed Player1 from the keepInventory list." |
| A13 | `/keepinv remove Player1` again | "Player1 was not on the keepInventory list." |
| A14 | Tab-complete `/keepinv ` | Suggests `add`, `remove`, `list` |
| A15 | Tab-complete `/keepinv add ` | Suggests online player names |
| A16 | Tab-complete `/keepinv add Player1 ` | Suggests `all` |
| A17 | `/keepinv add Player1`, then tab-complete `/keepinv remove ` | Suggests Player1 (resolved from UUID, even if offline) |
| A18 | As non-op, run any `/keepinv` subcommand | Server denies (insufficient permission) |
| A19 | Add Player1, restart server, run `/keepinv list` | Player1 still listed (persistence) |
| A20 | Inspect `<world>/../config/selectivekeepinv/players.json` | Map of UUID to mode string |

## B. Death behavior in DEFAULT mode

Set up: `/keepinv add Player1` (no `all` flag). Confirm `/gamerule keepInventory false`.

### B1. Level 0 death

| #    | Step | Expected |
|------|------|----------|
| B1.1 | Player1 at level 0, give a varied inventory, kill with `/kill` | Everything drops normally (items on the ground, no XP orbs because level 0) |
| B1.2 | Respawn, check inventory | Empty |
| B1.3 | Chat message on respawn | `[SelectiveKeepInv]` line, `You Died at level 0.`, third line is a same-dim mock with distance, no XP-roll line |

### B2. Hotbar tier (level 1-9)

| #    | Step | Expected |
|------|------|----------|
| B2.1 | Player1 at level 5, hotbar slots all filled, main inv full, armor full, kill | Slots 1-5 preserved. Slots 6-9, main inv, armor all drop |
| B2.2 | Player1 at level 9, hotbar all filled | All 9 hotbar slots preserved. Main inv, armor still drop |

### B3. Offhand + armor unlocks

| #    | Step | Expected |
|------|------|----------|
| B3.1 | Player1 at level 10 | + offhand preserved |
| B3.2 | Player1 at level 11 | + helmet preserved |
| B3.3 | Player1 at level 16 | + chestplate preserved |
| B3.4 | Player1 at level 21 | + leggings preserved |
| B3.5 | Player1 at level 25 (between thresholds) | Same as level 21 (boots still drop) |
| B3.6 | Player1 at level 26 | All armor preserved |

### B4. Accessories tier (level 50)

| #    | Step | Expected |
|------|------|----------|
| B4.1 | Player1 at level 49, baubles + trinkets equipped, kill | Baubles + trinkets drop |
| B4.2 | Player1 at level 50 | All baubles + all trinkets preserved on respawn |
| B4.3 | Verify on respawn: open inventory + baubles tab + trinkets GUI | Items intact in all three places |

### B5. Full inventory + XP carryover (level 100+)

| #    | Step | Expected |
|------|------|----------|
| B5.1 | Player1 at level 99, full inv kill | Hotbar + offhand + armor + baubles preserved, main inv 27 slots drop |
| B5.2 | Player1 at level 100 | Main inv also preserved. XP retained = 0 (level - 100 = 0) |
| B5.3 | Player1 at level 150, repeat 5-10 times | XP retained varies: should see roughly 50 (D=1), 25 (D=2), 16 (D=3). Distribution is uniform random. |
| B5.4 | Chat message on respawn at level 150 | Fourth line appears with XP-roll flavor matching the divisor pool |
| B5.5 | Player1 at level 200 | Full inv preserved, XP retained = 100 / D |
| B5.6 | Player1 at level 1000 | Full inv preserved, XP retained = 900 / D |

## C. Death behavior in ALL mode

Set up: `/keepinv add Player1 all`.

| #   | Step | Expected |
|-----|------|----------|
| C1  | Player1 at level 0, full inv, kill | Everything preserved including 0 XP. Nothing drops |
| C2  | Player1 at level 30 + 75% progress to level 31, kill | Respawn at level 30 with the same 75% partial progress preserved |
| C3  | No XP orbs spawn on death | Confirm by watching death location |
| C4  | Chat message on respawn | `[SelectiveKeepInv]` line, `You Died at level 30.`, third line is an ALL-mode praise line with no distance number, no fourth line |
| C5  | Player1 at level 30 in ALL mode, check ALL-mode line variants | After several deaths should see WITH_XP flavor lines |
| C6  | Player1 at level 0 in ALL mode | NO_XP flavor lines may appear |

## D. Death behavior for non-listed players

Set up: confirm Player2 is not on the list (`/keepinv list`).

| #  | Step | Expected |
|----|------|----------|
| D1 | Give Player2 inventory + XP, kill | Items drop, XP orbs spawn, or Corail Tombstone grave forms (RLCraft default) |
| D2 | Player2 respawns | Inventory empty unless recovered from grave / floor |
| D3 | No `[SelectiveKeepInv]` chat message | Confirm |
| D4 | Kill Player1 (listed) right after | Player1 still keeps inventory per their mode. Listed and unlisted players don't interfere |

## E. Mode switching

| #  | Step | Expected |
|----|------|----------|
| E1 | `/keepinv add Player1`, kill at level 5 | Hotbar 1-5 preserved (default mode) |
| E2 | `/keepinv add Player1 all`, kill at level 5 | Everything preserved including XP (all mode) |
| E3 | `/keepinv remove Player1`, kill | Drops normally (off the list) |
| E4 | `/keepinv add Player1`, restart server, kill | Default mode behavior intact across restart |

## F. Cross-dimension death

| #  | Step | Expected |
|----|------|----------|
| F1 | Listed Player1 (any mode) dies in the Nether, respawns in the overworld | Items / XP preserved per mode |
| F2 | Chat message after F1 | Third line is a DIFF_DIM variant (no distance number) |
| F3 | F1 in the End, jumping off the island | Same as F1 |
| F4 | F1 in the Twilight Forest or other modded dimension | Same as F1 |

## G. Death cause variants

These are RLCraft Dregora flavor checks. Listed Player1 at level 20 (DEFAULT mode) for all rows.

| #  | Step | Expected |
|----|------|----------|
| G1 | Killed by skeleton arrows | Items preserved per plan |
| G2 | Killed by fall damage | Items preserved per plan |
| G3 | Killed by lava | Items preserved per plan (no burning, nothing drops for kept slots) |
| G4 | Killed by thirst | Items preserved per plan |
| G5 | Killed by freezing | Items preserved per plan |
| G6 | Killed by a Lycanites boss AOE | Items preserved per plan. Any other player caught in the AOE behaves normally (not affected by Player1's preservation) |
| G7 | Killed by a parasite | Items preserved per plan |

## H. Edge cases

| #  | Step | Expected |
|----|------|----------|
| H1 | Listed player dies in creative mode | No errors. Creative doesn't drop anyway |
| H2 | Listed player dies in spectator (if you can get there) | No errors. Spectator path is skipped per S2 fix |
| H3 | Two listed players die in the same server tick | Both keep their own inventory, both get their own death message, no cross-talk |
| H4 | Listed player at level 100 exactly | XP retained = 0. NO XP-roll flavor line on respawn (gated on `xpRetained > 0`) |
| H5 | Listed player respawns at the exact death position (rare) | Distance reads `0 blocks` |
| H6 | Server log during any listed death | No stack traces. No `[ERROR]` lines from `selectivekeepinv` |
| H7 | Player joins, gets added, dies before any restart | Works. UUID is in profile cache as soon as they connect |

## What passing looks like

- A1-A20 each match the expected column exactly.
- For B-rows: respawn screen, click respawn, check inventory tab. Slots match
  the plan. XP level matches the threshold table.
- For C-rows: open inventory tab AND the baubles/trinkets GUI. Everything
  intact. XP level + bar fill identical to pre-death.
- For D-rows: vanilla / RLCraft default death behavior, unchanged.
- For F-rows: items preserved, message switches to the cross-dimension pool.
- For G-rows: every RLCraft death cause triggers preservation. If anything
  doesn't, capture the death scenario and the mod might need a more aggressive
  event hook.
- No `selectivekeepinv` log lines beyond the load message and (if encountered)
  the malformed-JSON warning.

## Known caveats to verify

- **Curse of Vanishing.** Vanilla destroys vanishing-cursed items in
  `EntityPlayer.onDeath()` before `LivingDeathEvent` fires in some code
  paths. Confirm a vanishing-cursed sword still vanishes for a listed
  player. Acceptable in most cases.
- **Tombstone interaction.** For listed players we clear slots before
  vanilla's drop block fires. Tombstone shouldn't make a grave because
  `PlayerDropsEvent` either sees an empty drops list (all mode) or fires
  with only unpreserved items (default mode). Verify no Tombstone grave
  appears for `all` mode and a partial-loot grave for `default` mode.
- **Baubley Elytra.** Has its own HIGHEST `PlayerDropsEvent` handler that
  clears bauble slots. We snapshot in `LivingDeathEvent` which fires first,
  so we get the slot contents before Baubley Elytra touches them. Confirm
  with a Baubley Elytra in the body slot that it survives in `all` mode and
  at level 50+ in `default` mode.
