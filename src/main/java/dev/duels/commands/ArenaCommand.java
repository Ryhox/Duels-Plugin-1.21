package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArenaCommand implements CommandExecutor, TabCompleter {

    private final DuelsPlugin plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "setfirst", "setsecond", "setcorner1", "setcorner2",
            "create", "list", "delete", "info", "testreset",
            "kit", "listbindings"
    );

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
            case "setfirst"      -> handleSetFirst(player, args);
            case "setsecond"     -> handleSetSecond(player, args);
            case "setcorner1"    -> handleSetCorner1(player, args);
            case "setcorner2"    -> handleSetCorner2(player, args);
            case "create"        -> handleCreate(player, args);
            case "list"          -> handleList(player);
            case "delete"        -> handleDelete(player, args);
            case "info"          -> handleInfo(player, args);
            case "testreset"     -> handleTestReset(player, args);
            case "kit"           -> handleKitBinding(player, args);
            case "listbindings"  -> handleListBindings(player);
            default -> {
                player.sendMessage(plugin.getPrefix() + "§cUnknown subcommand!");
                showUsage(player);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("duels.admin")) return List.of();

        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            return switch (sub) {
                case "kit" -> filter(new ArrayList<>(plugin.getKitManager().getKitNames()), args[1]);
                case "list", "listbindings" -> List.of();
                default -> filter(plugin.getArenaManager().getArenaNames(), args[1]);
            };
        }

        if (args.length == 3 && sub.equals("kit")) {
            List<String> options = new ArrayList<>(plugin.getArenaManager().getArenaNames());
            options.add("none");
            return filter(options, args[2]);
        }

        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void showUsage(Player player) {
        player.sendMessage(plugin.getPrefix() + "§7Usage: /arena <subcommand> [args]");
        player.sendMessage(plugin.getPrefix() + "§7Subcommands:");
        player.sendMessage("  §a/arena setfirst <name>     §8— §7Set spawn 1");
        player.sendMessage("  §a/arena setsecond <name>    §8— §7Set spawn 2");
        player.sendMessage("  §a/arena setcorner1 <name>   §8— §7Set corner 1");
        player.sendMessage("  §a/arena setcorner2 <name>   §8— §7Set corner 2");
        player.sendMessage("  §a/arena create <name>       §8— §7Save arena & snapshot");
        player.sendMessage("  §a/arena list                §8— §7List all arenas");
        player.sendMessage("  §a/arena delete <name>       §8— §7Delete an arena");
        player.sendMessage("  §a/arena info <name>         §8— §7Show arena info");
        player.sendMessage("  §a/arena testreset <name>    §8— §7Test arena reset");
        player.sendMessage("  §a/arena kit <kit> <arena>   §8— §7Bind a kit to an arena");
        player.sendMessage("  §a/arena listbindings        §8— §7List all kit-arena bindings");
    }

    private void handleSetFirst(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena setfirst <arenaName>");
            return;
        }

        String arenaName = args[1];
        Location location = player.getLocation();

        plugin.getArenaManager().setArenaSpawn1(arenaName, location);
        player.sendMessage(plugin.getPrefix() + "§aFirst spawn point set for arena: §e" + arenaName);
    }

    private void handleSetSecond(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena setsecond <arenaName>");
            return;
        }

        String arenaName = args[1];
        Location location = player.getLocation();

        plugin.getArenaManager().setArenaSpawn2(arenaName, location);
        player.sendMessage(plugin.getPrefix() + "§aSecond spawn point set for arena: §e" + arenaName);
    }

    private void handleSetCorner1(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena setcorner1 <arenaName>");
            return;
        }

        String arenaName = args[1];
        Location location = player.getLocation();

        plugin.getArenaManager().setArenaCorner1(arenaName, location);
        player.sendMessage(plugin.getPrefix() + "§aFirst corner set for arena: §e" + arenaName);
    }

    private void handleSetCorner2(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena setcorner2 <arenaName>");
            return;
        }

        String arenaName = args[1];
        Location location = player.getLocation();

        plugin.getArenaManager().setArenaCorner2(arenaName, location);
        player.sendMessage(plugin.getPrefix() + "§aSecond corner set for arena: §e" + arenaName);
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena create <arenaName>");
            return;
        }

        String arenaName = args[1];

        if (plugin.getArenaManager().createArena(arenaName)) {
            player.sendMessage(plugin.getPrefix() + "§aArena '§e" + arenaName + "§a' created! Snapshot saved.");
        } else {
            player.sendMessage(plugin.getPrefix() + "§cCould not create arena '§e" + arenaName + "§c'!");
            player.sendMessage(plugin.getPrefix() + "§7Make sure you set: §asetfirst §7+ §asetsecond §7+ §asetcorner1 §7+ §asetcorner2");
        }
    }

    private void handleList(Player player) {
        java.util.List<String> arenas = plugin.getArenaManager().getArenaNames();

        if (arenas.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + "§cNo arenas created yet!");
            return;
        }

        player.sendMessage(plugin.getPrefix() + "§6Available Arenas §7(" + arenas.size() + "):");
        for (String arena : arenas) {
            dev.duels.objects.Arena a = plugin.getArenaManager().getArena(arena);
            String status = (a != null && a.isInUse()) ? "§c[In Use]" : "§a[Free]";
            player.sendMessage("  §7- §e" + arena + " " + status);
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena delete <arenaName>");
            return;
        }

        String arenaName = args[1];

        if (plugin.getArenaManager().deleteArena(arenaName)) {
            player.sendMessage(plugin.getPrefix() + "§aArena '§e" + arenaName + "§a' deleted!");
        } else {
            player.sendMessage(plugin.getPrefix() + "§cCould not delete '§e" + arenaName + "§c'! It might be in use or doesn't exist.");
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
            player.sendMessage(plugin.getPrefix() + "§cArena not found: §e" + arenaName);
            return;
        }

        player.sendMessage(plugin.getPrefix() + "§6Arena Info: §e" + arenaName);
        player.sendMessage("  §7Status:   " + (arena.isInUse() ? "§cIn Use" : "§aAvailable"));
        player.sendMessage("  §7Spawn 1:  " + (arena.getSpawn1() != null ? "§aSet" : "§cNot Set"));
        player.sendMessage("  §7Spawn 2:  " + (arena.getSpawn2() != null ? "§aSet" : "§cNot Set"));
        player.sendMessage("  §7Corners:  " + (arena.getCorner1() != null && arena.getCorner2() != null ? "§aSet" : "§cNot Set"));
        player.sendMessage("  §7Snapshot: " + (arena.hasSnapshot() ? "§aSaved" : "§cNot Saved"));

        List<String> boundKits = plugin.getArenaManager().getAllKitArenaBindings().entrySet().stream()
                .filter(e -> arenaName.equals(e.getValue()))
                .map(e -> plugin.getKitManager().getKitDisplayName(e.getKey()))
                .collect(Collectors.toList());
        if (!boundKits.isEmpty()) {
            player.sendMessage("  §7Bound kits §8(" + boundKits.size() + ")§7: §b" + String.join("§7, §b", boundKits));
        }
    }

    private void handleTestReset(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena testreset <arenaName>");
            return;
        }

        String arenaName = args[1];
        dev.duels.objects.Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null) {
            player.sendMessage(plugin.getPrefix() + "§cArena not found: §e" + arenaName);
            return;
        }

        if (arena.isInUse()) {
            player.sendMessage(plugin.getPrefix() + "§cArena is currently in use!");
            return;
        }

        if (!arena.hasSnapshot()) {
            player.sendMessage(plugin.getPrefix() + "§cThis arena has no snapshot! Run §a/arena create " + arenaName + " §cfirst.");
            return;
        }

        player.sendMessage(plugin.getPrefix() + "§eTesting arena reset for: §b" + arenaName);

        arena.setInUse(true);

        plugin.getArenaManager().resetArena(arena, () -> {
            arena.setInUse(false);
            player.sendMessage(plugin.getPrefix() + "§aArena reset test completed!");
        });
    }

    private void handleKitBinding(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /arena kit <kitname> <arenaname|none>");
            player.sendMessage(plugin.getPrefix() + "§7Use §enone §7as arenaname to remove the binding.");
            return;
        }

        String kitId = args[1];
        String arenaName = args[2];

        if (!plugin.getKitManager().kitExists(kitId)) {
            player.sendMessage(plugin.getPrefix() + "§cKit '§e" + kitId + "§c' does not exist!");
            player.sendMessage(plugin.getPrefix() + "§7Available kits: §e" + String.join("§7, §e", plugin.getKitManager().getKitNames()));
            return;
        }

        if (arenaName.equalsIgnoreCase("none")) {
            plugin.getArenaManager().setKitArenaBinding(kitId, null);
            String kitDisplay = plugin.getKitManager().getKitDisplayName(kitId);
            player.sendMessage(plugin.getPrefix() + "§aRemoved arena binding for kit §r" + kitDisplay + "§a.");
            return;
        }

        if (plugin.getArenaManager().getArena(arenaName) == null) {
            player.sendMessage(plugin.getPrefix() + "§cArena '§e" + arenaName + "§c' does not exist!");
            player.sendMessage(plugin.getPrefix() + "§7Available arenas: §e" + String.join("§7, §e", plugin.getArenaManager().getArenaNames()));
            return;
        }

        plugin.getArenaManager().setKitArenaBinding(kitId, arenaName);

        String kitDisplay = plugin.getKitManager().getKitDisplayName(kitId);
        player.sendMessage(plugin.getPrefix() + "§aKit §r" + kitDisplay + " §awill now always use arena §b" + arenaName + "§a!");
        player.sendMessage(plugin.getPrefix() + "§7Players selecting this kit in /duel will be placed in this arena.");
    }

    private void handleListBindings(Player player) {
        Map<String, String> bindings = plugin.getArenaManager().getAllKitArenaBindings();

        if (bindings.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + "§7No kit-arena bindings set.");
            player.sendMessage(plugin.getPrefix() + "§7Use §a/arena kit <kit> <arena> §7to bind a kit.");
            return;
        }

        player.sendMessage(plugin.getPrefix() + "§6Kit → Arena Bindings §7(" + bindings.size() + "):");
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            String kitDisplay = plugin.getKitManager().getKitDisplayName(entry.getKey());
            player.sendMessage("  §r" + kitDisplay + " §8→ §b" + entry.getValue());
        }
    }
}
