package com.flashminat0.selectivekeepinv;

import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /keepinv add <player>           add in DEFAULT mode (XP-cost preservation)
 * /keepinv add <player> all       add in ALL mode (keep everything, no cost)
 * /keepinv remove <player>        remove from list entirely
 * /keepinv list                   show all protected players + their modes
 * /keepinv list <player>          show one player's mode
 *
 * Requires op level 2.
 */
public class CommandKeepInv extends CommandBase {

    private static final String ALL_ARG = "all";

    @Override public String getName() { return "keepinv"; }

    @Override public String getUsage(ICommandSender sender) {
        return "/keepinv <add|remove|list> [player] [all]";
    }

    @Override public int getRequiredPermissionLevel() { return 2; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) throw new WrongUsageException(getUsage(sender));
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "list":
                handleList(server, sender, args);
                return;
            case "add":
                requireAtLeast(args, 2, sender);
                handleAdd(server, sender, args);
                return;
            case "remove":
                requireAtLeast(args, 2, sender);
                handleRemove(server, sender, args);
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private static void requireAtLeast(String[] args, int n, ICommandSender sender) throws CommandException {
        if (args.length < n) throw new WrongUsageException("/keepinv <add|remove|list> [player] [all]");
    }

    // ---------------------------------------------------------------------
    // add
    // ---------------------------------------------------------------------

    private void handleAdd(MinecraftServer server, ICommandSender sender, String[] args) {
        String targetName = args[1];
        GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(targetName);
        if (profile == null) {
            msg(sender, TextFormatting.RED + "Unknown player: " + targetName
                    + " (they need to have joined the server at least once).");
            return;
        }
        UUID uuid = profile.getId();

        Mode requested;
        if (args.length == 2) {
            requested = Mode.DEFAULT;
        } else if (args.length == 3 && args[2].equalsIgnoreCase(ALL_ARG)) {
            requested = Mode.ALL;
        } else {
            msg(sender, TextFormatting.RED + "Usage: /keepinv add <player> [all]");
            return;
        }

        PlayerList.SetResult result = PlayerList.set(uuid, requested);
        switch (result) {
            case ADDED:
                msg(sender, TextFormatting.GREEN + "Added " + targetName
                        + " in " + requested.getName() + " mode.");
                break;
            case SWITCHED:
                Mode previous = (requested == Mode.ALL) ? Mode.DEFAULT : Mode.ALL;
                msg(sender, TextFormatting.GREEN + "Switched " + targetName
                        + " from " + previous.getName() + " to " + requested.getName() + " mode.");
                break;
            case UNCHANGED:
                msg(sender, TextFormatting.YELLOW + targetName
                        + " is already on the list in " + requested.getName() + " mode.");
                break;
        }
    }

    // ---------------------------------------------------------------------
    // remove
    // ---------------------------------------------------------------------

    private void handleRemove(MinecraftServer server, ICommandSender sender, String[] args) {
        String targetName = args[1];
        GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(targetName);
        if (profile == null) {
            msg(sender, TextFormatting.RED + "Unknown player: " + targetName);
            return;
        }
        if (PlayerList.remove(profile.getId())) {
            msg(sender, TextFormatting.GREEN + "Removed " + targetName + " from the keepInventory list.");
        } else {
            msg(sender, TextFormatting.YELLOW + targetName + " was not on the keepInventory list.");
        }
    }

    // ---------------------------------------------------------------------
    // list
    // ---------------------------------------------------------------------

    private void handleList(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length > 2) {
            msg(sender, TextFormatting.RED + "Usage: /keepinv list [player]");
            return;
        }
        if (args.length == 2) {
            String name = args[1];
            GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(name);
            if (profile == null) {
                msg(sender, TextFormatting.RED + "Unknown player: " + name);
                return;
            }
            Mode m = PlayerList.getMode(profile.getId());
            if (m == null) {
                msg(sender, TextFormatting.YELLOW + name + " is not on the list.");
            } else {
                msg(sender, TextFormatting.YELLOW + name + ": "
                        + TextFormatting.GRAY + m.getName());
            }
            return;
        }

        Map<UUID, Mode> all = PlayerList.all();
        if (all.isEmpty()) {
            msg(sender, TextFormatting.YELLOW + "No players currently have keepInventory enabled.");
            return;
        }
        msg(sender, TextFormatting.YELLOW + "Players with keepInventory enabled:");
        for (Map.Entry<UUID, Mode> e : all.entrySet()) {
            String name = e.getKey().toString();
            GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(e.getKey());
            if (profile != null && profile.getName() != null) name = profile.getName();
            msg(sender, TextFormatting.GRAY + " - " + name + ": " + e.getValue().getName());
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static void msg(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(text));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
                                          String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "add", "remove", "list");
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            if (sub.equals("add") || sub.equals("list")) {
                return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
            }
            if (sub.equals("remove")) {
                // For remove, suggest players ACTUALLY on the list (resolving
                // UUID -> name via profile cache). Falls back to online names
                // for any UUID we can't resolve.
                return getListOfStringsMatchingLastWord(args, listedPlayerNames(server));
            }
        }
        if (args.length == 3 && sub.equals("add")) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList(ALL_ARG));
        }
        return Collections.emptyList();
    }

    private static List<String> listedPlayerNames(MinecraftServer server) {
        List<String> names = new ArrayList<>();
        for (UUID id : PlayerList.all().keySet()) {
            GameProfile profile = server.getPlayerProfileCache().getProfileByUUID(id);
            if (profile != null && profile.getName() != null) {
                names.add(profile.getName());
            }
        }
        return names;
    }
}
