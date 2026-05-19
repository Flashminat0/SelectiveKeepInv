package com.flashminat0.selectivekeepinv;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

import java.util.List;
import java.util.Random;


/**
 * Mode-aware preservation.
 *
 * <h2>Death (LivingDeathEvent, HIGHEST)</h2>
 * Resolve the {@link PreservationPlan} from the player's mode + death XP level.
 * For each piece the plan says to preserve, copy it into player NBT and clear
 * the live slot. Vanilla then continues into EntityPlayer.onDeath:
 * dropAllItems iterates the now-partially-empty inventory and drops only what
 * we did NOT clear. Same for Baubles + Trinkets: their PlayerDropsEvent
 * handlers see empty slots and drop nothing.
 *
 * <h2>XP drops (LivingExperienceDropEvent, HIGHEST)</h2>
 * Cancelled only in {@link Mode#ALL}. In {@link Mode#DEFAULT} we let vanilla
 * drop XP orbs so the player can run back and recover some.
 *
 * <h2>Respawn (PlayerEvent.Clone, wasDeath=true)</h2>
 * Read snapshot off the old entity, write each preserved piece onto the new
 * player. Set XP level to plan.xpRetained. Copy the small "respawn message"
 * payload onto the new player.
 *
 * <h2>After respawn (PlayerEvent.PlayerRespawnEvent)</h2>
 * Read the respawn-message payload, compute distance from current (respawn)
 * position to the death position, send the chat message, clear the payload.
 */
public class EventHandler {

    /** Snapshot tag on the dying entity. */
    private static final String NBT_KEY = "selectivekeepinv_data";
    /** Respawn-message tag carried forward to the new player. */
    private static final String NBT_MSG_KEY = "selectivekeepinv_message";

    /** Single shared RNG. Minecraft's server tick is single-threaded. */
    private static final Random RANDOM = new Random();

    // InventoryPlayer slot index layout:
    //   0..8    hotbar
    //   9..35   main inventory
    //   100..103 armor (boots=100, leggings=101, chestplate=102, helmet=103)
    //   150      offhand
    private static final int HOTBAR_END_EXCLUSIVE = 9;
    private static final int MAIN_INV_END_EXCLUSIVE = 36;
    private static final int ARMOR_BOOTS      = 0;
    private static final int ARMOR_LEGGINGS   = 1;
    private static final int ARMOR_CHESTPLATE = 2;
    private static final int ARMOR_HELMET     = 3;
    private static final int ARMOR_SLOT_OFFSET = 100;
    private static final int OFFHAND_SLOT      = 150;

    // -----------------------------------------------------------------
    // Death
    // -----------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        Config cfg = SelectiveKeepInv.config;
        // Vanilla skips inventory drops entirely for spectators (they have no
        // inventory in the conventional sense). Mirror that: don't snapshot,
        // don't clear, don't queue a death message.
        if (cfg.skipSpectators && player.isSpectator()) return;

        Mode mode = PlayerList.getMode(player.getUniqueID());
        if (mode == null) return;

        int xpLevel = Math.max(0, player.experienceLevel);
        // Random divisor uniformly in [divisorMin, divisorMax] for the
        // XP-carryover gamble at level >= xpCarryoverThreshold. Rolled once
        // per death so the value on respawn is deterministic from then on.
        int xpDivisor = rollDivisor(cfg);
        PreservationPlan plan = (mode == Mode.ALL)
                ? PreservationPlan.all(xpLevel)
                : PreservationPlan.resolveDefault(xpLevel, xpDivisor, cfg);

        NBTTagCompound data = new NBTTagCompound();
        data.setString ("Mode",        mode.getName());
        data.setInteger("DeathLevel",  xpLevel);
        data.setInteger("XpRetained",  plan.xpRetained);
        data.setInteger("XpDivisor",   xpDivisor);
        // Full XP fidelity: ALL mode restores all three, DEFAULT mode only uses
        // XpRetained (level).
        data.setFloat  ("XpProgress",  player.experience);
        data.setInteger("XpTotal",     player.experienceTotal);
        data.setDouble ("DeathX",      player.posX);
        data.setDouble ("DeathY",      player.posY);
        data.setDouble ("DeathZ",      player.posZ);
        data.setInteger("DeathDim",    player.dimension);

        InventoryPlayer inv = player.inventory;
        NBTTagList invList = new NBTTagList();

        // Hotbar: first N slots from the left.
        for (int i = 0; i < plan.hotbarSlots; i++) {
            snapshotAndClearMain(inv, i, invList);
        }
        // Main inventory: the 3x9 backpack above the hotbar.
        if (plan.mainInventory) {
            for (int i = HOTBAR_END_EXCLUSIVE; i < MAIN_INV_END_EXCLUSIVE; i++) {
                snapshotAndClearMain(inv, i, invList);
            }
        }
        // Armor: per-piece.
        if (plan.helmet)     snapshotAndClearArmor(inv, ARMOR_HELMET,     invList);
        if (plan.chestplate) snapshotAndClearArmor(inv, ARMOR_CHESTPLATE, invList);
        if (plan.leggings)   snapshotAndClearArmor(inv, ARMOR_LEGGINGS,   invList);
        if (plan.boots)      snapshotAndClearArmor(inv, ARMOR_BOOTS,      invList);
        // Offhand.
        if (plan.offhand) {
            for (int i = 0; i < inv.offHandInventory.size(); i++) {
                snapshotAndClear(inv.offHandInventory, i, invList, (byte) (i + OFFHAND_SLOT));
            }
        }
        if (invList.tagCount() > 0) data.setTag("Inventory", invList);

        // Accessories: Baubles + Trinkets, gated by Loader checks.
        if (plan.accessories) {
            if (Loader.isModLoaded("baubles")) {
                BaublesCompat.saveBaubles(player, data);
                BaublesCompat.clearBaubles(player);
            }
            if (Loader.isModLoaded("xat")) {
                TrinketsCompat.saveTrinkets(player, data);
                TrinketsCompat.clearTrinkets(player);
            }
        }

        player.getEntityData().setTag(NBT_KEY, data);
    }

    // -----------------------------------------------------------------
    // XP orb drop suppression
    // -----------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onXpDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        if (!SelectiveKeepInv.config.allModeCancelsXpDrops) return;
        if (PlayerList.getMode(player.getUniqueID()) == Mode.ALL) {
            event.setCanceled(true);
        }
    }

    // -----------------------------------------------------------------
    // Respawn (clone): restore + carry message forward
    // -----------------------------------------------------------------

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        EntityPlayer original  = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();

        NBTTagCompound entityData = original.getEntityData();
        if (!entityData.hasKey(NBT_KEY)) return;

        NBTTagCompound data = entityData.getCompoundTag(NBT_KEY);

        if (data.hasKey("Inventory")) {
            NBTTagList invList = data.getTagList("Inventory", 10);
            newPlayer.inventory.readFromNBT(invList);
        }

        // XP restore depends on mode:
        //   ALL:    full fidelity (level + partial progress + total all preserved).
        //   DEFAULT: only the retained integer level survives (rest is the cost).
        Mode mode = Mode.fromName(data.getString("Mode"));
        int xpRetained = data.getInteger("XpRetained");
        if (mode == Mode.ALL) {
            newPlayer.experienceLevel = xpRetained;
            newPlayer.experience      = data.getFloat("XpProgress");
            newPlayer.experienceTotal = data.getInteger("XpTotal");
        } else {
            newPlayer.experienceLevel = xpRetained;
            newPlayer.experience      = 0f;
            newPlayer.experienceTotal = 0;
        }

        if (Loader.isModLoaded("baubles")) {
            BaublesCompat.restoreBaubles(newPlayer, data);
        }
        if (Loader.isModLoaded("xat")) {
            TrinketsCompat.restoreTrinkets(newPlayer, data);
        }

        // Carry only the message-relevant subset onto the new player so the
        // respawn handler can find it. (Original entity is gone after respawn.)
        NBTTagCompound msg = new NBTTagCompound();
        msg.setString ("Mode",       data.getString("Mode"));
        msg.setInteger("DeathLevel", data.getInteger("DeathLevel"));
        msg.setInteger("XpRetained", xpRetained);
        msg.setInteger("XpDivisor",  data.getInteger("XpDivisor"));
        msg.setDouble ("DeathX",     data.getDouble("DeathX"));
        msg.setDouble ("DeathY",     data.getDouble("DeathY"));
        msg.setDouble ("DeathZ",     data.getDouble("DeathZ"));
        msg.setInteger("DeathDim",   data.getInteger("DeathDim"));
        newPlayer.getEntityData().setTag(NBT_MSG_KEY, msg);

        // Done with the original snapshot.
        entityData.removeTag(NBT_KEY);
    }

    // -----------------------------------------------------------------
    // Respawn (after world re-entry): chat message
    //
    // Message line pools live in DeathMessages. They're static-imported
    // above so the references below stay unqualified.
    // -----------------------------------------------------------------

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        EntityPlayer player = event.player;
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(NBT_MSG_KEY)) return;
        NBTTagCompound msg = entityData.getCompoundTag(NBT_MSG_KEY);
        entityData.removeTag(NBT_MSG_KEY);

        // Messages globally disabled: nothing more to do.
        if (!SelectiveKeepInv.config.messagesEnabled) return;

        Mode mode = Mode.fromName(msg.getString("Mode"));
        int deathLevel = msg.getInteger("DeathLevel");

        StringBuilder sb = new StringBuilder();
        sb.append(TextFormatting.AQUA).append("[SelectiveKeepInv]\n")
          .append(TextFormatting.GRAY).append("You Died at level ")
          .append(TextFormatting.YELLOW).append(deathLevel)
          .append(TextFormatting.GRAY).append(".\n");

        DeathMessageStore msgs = SelectiveKeepInv.deathMessages;
        if (mode == Mode.ALL) {
            String[] extra = (deathLevel > 0) ? msgs.allLinesWithXp : msgs.allLinesNoXp;
            sb.append(randomFromTwo(msgs.allLines, extra));
        } else {
            int deathDim = msg.getInteger("DeathDim");
            if (player.dimension != deathDim) {
                sb.append(randomFrom(msgs.diffDimLines));
            } else {
                // Horizontal (XZ) distance: players walk, they don't fly. A
                // 60-block-deep death right under the bed shouldn't read as
                // "60 blocks away".
                double dx = player.posX - msg.getDouble("DeathX");
                double dz = player.posZ - msg.getDouble("DeathZ");
                int dist  = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
                String coloredDist = TextFormatting.YELLOW + Integer.toString(dist)
                        + TextFormatting.GRAY;
                sb.append(String.format(randomFrom(msgs.sameDimLines), coloredDist));
            }

            // Fourth line: XP-roll flavor, only when XP was actually retained.
            // At deathLevel == xpCarryoverThreshold exactly, retained = 0 for
            // any divisor, so there's no "lucky" or "brutal" outcome to
            // describe; the line would lie about the result.
            int xpRetainedForMsg = msg.getInteger("XpRetained");
            if (SelectiveKeepInv.config.showXpRollFlavor && xpRetainedForMsg > 0) {
                int divisor = msg.getInteger("XpDivisor");
                String[] pool = (divisor <= 1) ? msgs.xpRollLucky
                              : (divisor == 2) ? msgs.xpRollMid
                                               : msgs.xpRollBrutal;
                sb.append("\n").append(TextFormatting.GRAY).append(randomFrom(pool));
            }
        }

        player.sendMessage(new TextComponentString(sb.toString()));
    }

    private static String randomFrom(String[] options) {
        return options[RANDOM.nextInt(options.length)];
    }

    /** Uniform-random pick across the concatenation of two arrays. */
    private static String randomFromTwo(String[] a, String[] b) {
        int idx = RANDOM.nextInt(a.length + b.length);
        return idx < a.length ? a[idx] : b[idx - a.length];
    }

    /**
     * Roll the XP-carryover divisor uniformly in [cfg.divisorMin, cfg.divisorMax]
     * inclusive. Clamps bounds defensively: if max &lt; min the value reverts
     * to whichever clamp is sane (min, clamped >= 1).
     */
    private static int rollDivisor(Config cfg) {
        int lo = Math.max(1, cfg.divisorMin);
        int hi = Math.max(lo, cfg.divisorMax);
        return lo + RANDOM.nextInt(hi - lo + 1);
    }

    // -----------------------------------------------------------------
    // Login: broadcast queued startup errors to ops.
    //
    // The queue is populated during preInit by Config.load and the
    // death-msgs override path. Anything in there means an admin should
    // open latest.log or the config file. Surfacing it in-chat catches
    // people who never look at the log.
    // -----------------------------------------------------------------

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;

        List<String> errors = SelectiveKeepInv.snapshotStartupErrors();
        if (errors.isEmpty()) return;

        // Only show to ops. canSendCommands is what CommandBase uses for the
        // permission-level >= 2 check.
        net.minecraft.server.MinecraftServer server = player.getServer();
        if (server == null) return;
        if (!server.getPlayerList().canSendCommands(player.getGameProfile())) return;

        for (String err : errors) {
            String line = TextFormatting.AQUA + "[SelectiveKeepInv] "
                    + TextFormatting.RED + "Startup warning: "
                    + TextFormatting.GRAY + err;
            player.sendMessage(new TextComponentString(line));
        }
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private static void snapshotAndClearMain(InventoryPlayer inv, int slot, NBTTagList out) {
        snapshotAndClear(inv.mainInventory, slot, out, (byte) slot);
    }

    private static void snapshotAndClearArmor(InventoryPlayer inv, int armorIdx, NBTTagList out) {
        snapshotAndClear(inv.armorInventory, armorIdx, out, (byte) (armorIdx + ARMOR_SLOT_OFFSET));
    }

    private static void snapshotAndClear(net.minecraft.util.NonNullList<ItemStack> list,
                                         int idx, NBTTagList out, byte nbtSlotId) {
        ItemStack stack = list.get(idx);
        if (stack.isEmpty()) return;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("Slot", nbtSlotId);
        stack.writeToNBT(tag);
        out.appendTag(tag);
        list.set(idx, ItemStack.EMPTY);
    }
}
