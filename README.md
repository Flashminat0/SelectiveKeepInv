# Selective Keep Inventory

Small server-side Forge mod for Minecraft 1.12.2 that lets specific players
keep their inventory on death without flipping the global `keepInventory`
gamerule for the whole server. Built against Forge 14.23.5.2860 for use
with RLCraft Dregora.

Server ops opt individual players into one of two preservation modes via
`/keepinv`. Players not on the list die normally and drop items as usual
(Corail Tombstone, vanilla, whatever you have handling drops).

## Commands

All require op (permission level 2):

```
/keepinv add <player>           add in default mode (XP-cost preservation)
/keepinv add <player> all       add in all mode (keep everything, no cost)
/keepinv remove <player>        remove from the list
/keepinv list                   list everyone protected
/keepinv list <player>          show one player's mode
```

The target player must have joined the server at least once. The mod resolves
their name to a UUID through the server's profile cache.

Re-running `add` with a different mode switches the player. Re-running with the
same mode just reports as already set. The list is saved to
`<server>/config/selectivekeepinv/players.json` and survives restarts.

## The two modes

**`all`** keeps everything on death: vanilla inventory, armor, offhand, baubles,
trinkets, and full XP including partial level progress. Nothing drops. No XP
orbs. No tombstone. Functionally identical to `keepInventory true` for that one
player.

**`default`** is the interesting one. What gets preserved depends on the
player's XP level at the moment of death. XP is the cost: it gets spent for
the things you keep. XP orbs still drop as usual so you can run back and grab
some of it.

### Default mode threshold table

Each row unlocks at that level and adds to everything below it.

| Level | Adds |
|------:|------|
| 1..9  | hotbar slots 1..N (left to right) |
| 10    | offhand |
| 11    | helmet |
| 16    | chestplate |
| 21    | leggings |
| 26    | boots (full armor) |
| 50    | accessories (baubles + trinkets combined) |
| 100   | the rest of the inventory (27 main slots) plus XP carryover |

The hotbar fills left to right. A level-5 death keeps slots 1-5 and drops 6-9.
The cost is fixed by the level you died at. Empty slots count toward it the
same way. No refunds for unused slots.

### XP carryover at level 100+

At level 100 and above you start getting XP back on respawn. The amount is a
gamble. On death the mod rolls a divisor of 1, 2, or 3, and you respawn with
`(deathLevel - 100) / D` XP. So at level 200 you could get 100, 50, or 33
back depending on the roll. You don't see the divisor directly. A flavor line
on respawn hints at how lucky (or not) the roll was.

## Baubles and trinkets support

If [Baubles](https://www.curseforge.com/minecraft/mc-mods/baubles) (Azanor's
7-slot one with amulet, head, body, two rings, belt, charm) is installed, the
accessories tier at level 50+ covers all 7 of those slots.

If [Trinkets and Baubles](https://www.curseforge.com/minecraft/mc-mods/trinkets-and-baubles)
by XzeroAir (modid `xat`) is installed, the same tier covers its extra trinket
slots too.

Other bauble-using mods route their items through the standard Baubles slots
so they're covered automatically: Bountiful Baubles, Baubley Elytra, etc.
SpartanHUD Baubles is client-side HUD overlay only, unaffected.

The mod runs fine without either of these installed. The accessory code just
doesn't fire.

## Death message

Listed players get a short chat message on respawn. Three lines, with a fourth
when XP carryover applies:

```
[SelectiveKeepInv]
You Died at level 150.
Your funeral procession is 312 blocks away.
Lucky roll. Most of your XP survived.
```

The lines are picked from pools. ALL mode gets light praise. Default mode in
the same dimension gets walking distance and mockery. Default mode after a
cross-dimension death gets extra savagery (your stuff is somewhere else, deal
with it). Pools live in `DeathMessages.java`. Edit freely.

## Install

1. Drop `selectivekeepinv-1.0.jar` into the server's `mods/` folder.
2. Start the server. Look for `Selective Keep Inventory` in the load log.
3. As op: `/keepinv add YourFriend` to protect someone.

Clients do not need the jar. It's marked `acceptableRemoteVersions = "*"` so
anyone can connect. Putting it client-side is harmless but does nothing useful.

Keep vanilla `/gamerule keepInventory` set to **false**. This mod runs
independently of that gamerule and only touches players on its own list.

## On-disk

```
<server>/config/selectivekeepinv/players.json
```

Plain JSON map from UUID to mode:

```json
{
  "550e8400-e29b-41d4-a716-446655440000": "default",
  "abc12345-...": "all"
}
```

Editable by hand if the server is stopped. Unknown mode strings are dropped on
load. Malformed JSON falls back to an empty list with a stack trace in the log
rather than crashing.

## Configuration (v2.0+)

The threshold values described above are no longer hardcoded. They live in
`config/selectivekeepinv/config.yml`, written automatically on first server
start with the v1.1 defaults. Edit and restart the server to apply.

```yaml
thresholds:
  hotbar-per-slot: 1          # XP cost per hotbar slot (leftmost first)
  offhand:        10
  helmet:         11
  chestplate:     16
  leggings:       21
  boots:          26
  accessories:    50
  main-inventory: 100
  xp-carryover:   100          # level at which the XP gamble begins

xp-carryover:
  divisor-min: 1               # set min = max = 1 to disable the gamble
  divisor-max: 3

behavior:
  skip-spectators: true
  all-mode-cancels-xp-drops: true

messages:
  enabled: true                # set false to suppress all mod chat output
  show-xp-roll-flavor: true    # fourth-line XP-roll hint on respawn
  override-corny-msgs: false   # use death-msgs.yml instead of built-in pools
```

Missing fields fall back to defaults silently (won't get a re-write). A
malformed file falls back to defaults entirely so the mod still loads, and
a warning is broadcast to op players on next login (also logged to
`latest.log` with a `[SelectiveKeepInv] WARN:` prefix). Reload requires a
server restart — there's no `/keepinv reload` yet.

### Custom death messages (v2.1+)

Set `messages.override-corny-msgs: true` in `config.yml`, then restart.
On first start with the flag enabled, the mod writes
`config/selectivekeepinv/death-msgs.yml` with the built-in pools so you
have a starting point. Edit, restart, done.

The file has eight sections, each a list of strings:

```yaml
all-lines:
  - "The reaper went home empty-handed."
  - "..."

all-lines-with-xp:           # extra praise when deathLevel > 0
  - "Loot AND levels intact. Disgusting."

all-lines-no-xp:             # extra mock for level-0 ALL deaths
  - "Nothing to drop, nothing to lose. Tragic."

same-dim-lines:              # %s is replaced with distance in blocks
  - "Your funeral procession is %s blocks away."

diff-dim-lines:              # used when respawn dimension != death dimension
  - "Your stuff is in another dimension. Cry about it."

xp-roll-lucky:               # divisor = 1
  - "The XP gods smiled today."

xp-roll-mid:                 # divisor = 2
  - "The XP roll was meh."

xp-roll-brutal:              # divisor = 3
  - "Brutal XP roll. Ouch."
```

Validation rules:

- All eight sections must be present.
- No pool may be empty.
- Every `same-dim-lines` entry must contain **exactly one** `%s` (and no
  other `%X` format specifier).
- No other pool may contain any `%X` format specifier.

If validation fails for any reason (missing section, empty pool, wrong
`%s` count, malformed YAML), the mod:

1. Reverts `override-corny-msgs` to `false` for this run.
2. Uses the built-in pools so nothing crashes.
3. Logs a `[SelectiveKeepInv] WARN:` line to `latest.log` explaining
   what's wrong (which section, which line number, what's expected).
4. Broadcasts the same warning to op players when they log in, so it
   surfaces even if you don't open the log.

Fix the file and restart to re-enable custom messages.

### Tuning tips

- **Restore the v1.0 progression** (XP carryover only at level 200+) by
  setting `xp-carryover: 200` and `main-inventory: 100` separately.
- **Disable the XP gamble** with `divisor-min: 1, divisor-max: 1`.
- **Effectively disable a tier** (e.g. don't preserve accessories at all)
  by setting that threshold to something absurd like `9999`.
- **Mute the death messages** entirely with `messages.enabled: false`.

## Build from source

You need:

1. **JDK 8**. Forge 1.12.2 will not build with newer Java.
   Adoptium Temurin 8 or Azul Zulu 8 both work. Set `JAVA_HOME` to point at it.
2. **Forge 1.12.2 MDK build 14.23.5.2860**. Download the Mdk zip from
   <https://files.minecraftforge.net/net/minecraftforge/forge/index_1.12.2.html>
   and extract it somewhere.

In the MDK:

1. Delete the example mod folder: `src/main/java/com/example/`.
2. Copy this repo's `src/main/java/com/flashminat0/` into the MDK's
   `src/main/java/com/`.
3. Copy `src/test/` from this repo into the MDK root (sibling of `src/main/`).
4. Replace the MDK's `src/main/resources/mcmod.info` with this repo's.
5. Make a `libs/` folder in the MDK root. Drop in `Baubles-1.12-1.5.2.jar` and
   `Trinkets and Baubles-0.32.5.jar` (compile-time only, won't be bundled).
6. Edit `build.gradle`:
   - Change `version`, `group`, and `archivesBaseName` to match this mod:
     ```groovy
     version = '1.0'
     group = 'com.flashminat0.selectivekeepinv'
     archivesBaseName = 'selectivekeepinv'
     ```
   - In the `dependencies { }` block, add:
     ```groovy
     compile fileTree(dir: 'libs', include: '*.jar')
     testCompile 'junit:junit:4.13.2'
     ```
   - At the bottom of the file, add:
     ```groovy
     test {
         testLogging {
             events 'passed', 'failed', 'skipped'
             showStandardStreams = true
             exceptionFormat = 'full'
         }
     }
     ```
7. From the MDK folder:
   ```
   ./gradlew test build       (macOS / Linux)
   gradlew test build         (Windows)
   ```

First run downloads Forge plus dependencies and takes a few minutes. The jar
lands in `build/libs/selectivekeepinv-1.0.jar`. Test HTML report is at
`build/reports/tests/test/index.html`.

## Known caveats

**Curse of Vanishing.** Vanilla destroys vanishing-cursed items inside
`EntityPlayer.onDeath()` which can run before our hook in some code paths.
Vanishing-cursed items may still be destroyed even for protected players.

**Other death-handling mods.** The mod clears slots in `LivingDeathEvent`
(HIGHEST priority) before vanilla's `dropAllItems()` runs. This means
`PlayerDropsEvent` handlers (Baubles drop logic, Trinkets drop logic, Corail
Tombstone, Baubley Elytra's drop fixer) see partially-empty inventories and
only operate on what wasn't kept. For unlisted players nothing changes:
vanilla drops, those mods do their thing.

**Concurrent deaths.** Each death is snapshotted and rolled independently on
the per-player NBT. No cross-contamination if two listed players die in the
same server tick.

**Cross-dimension death.** Items are preserved correctly even if you die in
the Nether (or Twilight Forest, End, etc.) and respawn in the overworld
(items live on the player entity, not in the world). The death-message
distance reading skips the distance number when the dimensions differ.

## Testing

Automated unit tests cover the deterministic pieces:

- `PlayerListTest` (18 tests) covers add/remove/getMode/SetResult semantics
  plus JSON persistence and graceful handling of bad files.
- `PreservationPlanTest` (33 tests) walks every threshold in the table and
  every divisor branch for XP carryover.
- `ModeTest` (6 tests) and `DeathMessagesTest` (11 tests) catch typos in
  enum names and the `%s` placeholders in the same-dim message pool.

Run `./gradlew test`. HTML report at `build/reports/tests/test/index.html`.

The runtime event flow (snapshot, clear, restore, message dispatch) is not
unit tested. Verify it on a dev server with `./gradlew runServer` and run
through `MANUAL_TESTING.md` before shipping changes.
