package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class QueueCommand implements CommandExecutor, TabCompleter {

    private final DuelsPlugin plugin;

    public QueueCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showUsage(player);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "leave" -> handleLeave(player);
            case "gui"   -> handleGUI(player);
            case "join"  -> handleJoin(player, args);
            default -> {
                player.sendMessage(plugin.getPrefix() + "§cUnknown subcommand!");
                showUsage(player);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("join", "leave", "gui"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return filter(new ArrayList<>(plugin.getKitManager().getKitNames()), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void showUsage(Player player) {
        player.sendMessage(plugin.getPrefix() + "§7Usage:");
        player.sendMessage("  §a/queue join <kit>  §8— §7Join a kit queue");
        player.sendMessage("  §a/queue leave       §8— §7Leave your current queue");
        player.sendMessage("  §a/queue gui         §8— §7Open the queue GUI");
    }

    private void handleLeave(Player player) {
        plugin.getQueueManager().leaveQueue(player);
        plugin.getPlayerManager().refreshQueueSlotItem(player);
    }

    private void handleGUI(Player player) {
        plugin.getGuiManager().openQueueGUI(player);
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: §a/queue join <kit>");
            return;
        }

        String kitName = args[1];

        if (!plugin.getKitManager().kitExists(kitName)) {
            player.sendMessage(plugin.getPrefix() + "§cKit not found: §e" + kitName);
            return;
        }

        // Toggle behavior
        if (plugin.getQueueManager().isInQueue(player.getUniqueId(), kitName)) {
            plugin.getQueueManager().leaveQueue(player);
            player.sendMessage(plugin.getPrefix() + "§cLeft §7queue for kit §e" + kitName);
        } else {
            plugin.getQueueManager().joinQueue(player, kitName);
        }

        plugin.getPlayerManager().refreshQueueSlotItem(player);
    }
}
