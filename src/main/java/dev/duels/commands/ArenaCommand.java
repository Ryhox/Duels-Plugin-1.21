package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public ArenaCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + "§cOnly players can use this command!");
            return true;
        }

        if (!player.hasPermission("duels.admin")) {
            player.sendMessage(plugin.getPrefix() + "§cYou don't have permission!");
            return true;
        }

        if (args.length < 1) {
            showUsage(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "setfirst" -> handleSetFirst(player, args);
            case "setsecond" -> handleSetSecond(player, args);
            case "setcorner1" -> handleSetCorner1(player, args);
            case "setcorner2" -> handleSetCorner2(player, args);
            case "create" -> handleCreate(player, args);
            case "list" -> handleList(player);
            case "delete" -> handleDelete(player, args);
            case "info" -> handleInfo(player, args);
            case "testreset" -> handleTestReset(player, args);
            default -> {
                player.sendMessage(plugin.getPrefix() + "§cUnknown subcommand!");
                showUsage(player);
            }
        }

        return true;
    }

    private void showUsage(Player player) {
        player.sendMessage(plugin.getPrefix() + "§7Usage: /arena <subcommand> [arenaName]");
        player.sendMessage(plugin.getPrefix() + "§7Subcommands:");
        player.sendMessage(plugin.getPrefix() + "§7  setfirst <arenaName> §8- §aSet first spawn point");
        player.sendMessage(plugin.getPrefix() + "§7  setsecond <arenaName> §8- §aSet second spawn point");
        player.sendMessage(plugin.getPrefix() + "§7  setcorner1 <arenaName> §8- §aSet first corner");
        player.sendMessage(plugin.getPrefix() + "§7  setcorner2 <arenaName> §8- §aSet second corner");
        player.sendMessage(plugin.getPrefix() + "§7  create <arenaName> §8- §aCreate arena (saves Snapshot)");
        player.sendMessage(plugin.getPrefix() + "§7  list §8- §aList all arenas");
        player.sendMessage(plugin.getPrefix() + "§7  delete <arenaName> §8- §cDelete an arena");
        player.sendMessage(plugin.getPrefix() + "§7  info <arenaName> §8- §eShow arena info");
        player.sendMessage(plugin.getPrefix() + "§7  testreset <arenaName> §8- §6Test arena reset");
    }

    private void handleSetFirst(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena setfirst <arenaName>");
            return;
        }

        String arenaName = args[1];
        Location location = player.getLocation();

        plugin.getArenaManager().setArenaSpawn1(arenaName, location);
        player.sendMessage(plugin.getPrefix() + "§aFirst spawn point set for arena: " + arenaName);
    }

    private void handleSetSecond(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena setsecond <arenaName>");
            return;
        }

        String arenaName = args[1];
        Location location = player.getLocation();

        plugin.getArenaManager().setArenaSpawn2(arenaName, location);
        player.sendMessage(plugin.getPrefix() + "§aSecond spawn point set for arena: " + arenaName);
    }

    private void handleSetCorner1(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena setcorner1 <arenaName>");
            return;
        }

        String arenaName = args[1];
        Location location = player.getLocation();

        plugin.getArenaManager().setArenaCorner1(arenaName, location);
        player.sendMessage(plugin.getPrefix() + "§aFirst corner set for arena: " + arenaName);
    }

    private void handleSetCorner2(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena setcorner2 <arenaName>");
            return;
        }

        String arenaName = args[1];
        Location location = player.getLocation();

        plugin.getArenaManager().setArenaCorner2(arenaName, location);
        player.sendMessage(plugin.getPrefix() + "§aSecond corner set for arena: " + arenaName);
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena create <arenaName>");
            return;
        }

        String arenaName = args[1];

        if (plugin.getArenaManager().createArena(arenaName)) {
            player.sendMessage(plugin.getPrefix() + "§aArena '" + arenaName + "' created! Snapshot saved.");
        } else {
            player.sendMessage(plugin.getPrefix() + "§cCould not create arena! Make sure all positions are set.");
        }
    }

    private void handleList(Player player) {
        java.util.List<String> arenas = plugin.getArenaManager().getArenaNames();

        if (arenas.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + "§cNo arenas created yet!");
            return;
        }

        player.sendMessage(plugin.getPrefix() + "§6Available Arenas:");
        for (String arena : arenas) {
            player.sendMessage("§7- §e" + arena);
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena delete <arenaName>");
            return;
        }

        String arenaName = args[1];

        if (plugin.getArenaManager().deleteArena(arenaName)) {
            player.sendMessage(plugin.getPrefix() + "§aArena '" + arenaName + "' deleted!");
        } else {
            player.sendMessage(plugin.getPrefix() + "§cCould not delete arena! It might be in use or doesn't exist.");
        }
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena info <arenaName>");
            return;
        }

        String arenaName = args[1];
        dev.duels.objects.Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(plugin.getPrefix() + "§cArena not found!");
            return;
        }

        player.sendMessage(plugin.getPrefix() + "§6Arena Info: §e" + arenaName);
        player.sendMessage(plugin.getPrefix() + "§7Status: " + (arena.isInUse() ? "§cIn Use" : "§aAvailable"));
        player.sendMessage(plugin.getPrefix() + "§7Spawn 1: " + (arena.getSpawn1() != null ? "§aSet" : "§cNot Set"));
        player.sendMessage(plugin.getPrefix() + "§7Spawn 2: " + (arena.getSpawn2() != null ? "§aSet" : "§cNot Set"));
        player.sendMessage(plugin.getPrefix() + "§7Corners: " +
                (arena.getCorner1() != null && arena.getCorner2() != null ? "§aSet" : "§cNot Set"));
        player.sendMessage(plugin.getPrefix() + "§7Snapshot: " + (arena.hasSnapshot() ? "§aSaved" : "§cNot Saved"));
    }

    private void handleTestReset(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena testreset <arenaName>");
            return;
        }

        String arenaName = args[1];
        dev.duels.objects.Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(plugin.getPrefix() + "§cArena not found!");
            return;
        }

        if (arena.isInUse()) {
            player.sendMessage(plugin.getPrefix() + "§cArena is currently in use!");
            return;
        }

        if (!arena.hasSnapshot()) {
            player.sendMessage(plugin.getPrefix() + "§cThis arena has no snapshot! Create it first.");
            return;
        }

        player.sendMessage(plugin.getPrefix() + "§eTesting arena reset for: " + arenaName);
        player.sendMessage(plugin.getPrefix() + "§7Restoring snapshot...");

        arena.setInUse(true);

        plugin.getArenaManager().resetArena(arena, () -> {
            arena.setInUse(false);
            player.sendMessage(plugin.getPrefix() + "§aArena reset test completed!");
        });
    }
}
