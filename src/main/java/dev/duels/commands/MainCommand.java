package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final DuelsPlugin plugin;

    public MainCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("duels.reload")) {
                sender.sendMessage(plugin.getPrefix() + "§cYou don't have permission!");
                return true;
            }

            reloadPlugin(sender);
            return true;
        }

        // Help message
        sender.sendMessage(plugin.getPrefix() + "§aDuels Plugin by Ryhox §8| §7All Commands:");
        sender.sendMessage("§a/duel <player> §8— §7Challenge a player (opens kit GUI)");
        sender.sendMessage("§a/accept §8— §7Accept a pending duel request");
        sender.sendMessage("§a/queue gui §8— §7Open queue kit selection");
        sender.sendMessage("§a/queue join <kit> §8— §7Join a kit queue");
        sender.sendMessage("§a/queue leave §8— §7Leave your current queue");
        sender.sendMessage("§a/kits §8— §7Browse available kits");
        sender.sendMessage("§a/previewkit <kitname> §8— §7Preview a kit in your inventory");
        sender.sendMessage("§a/stats [player] §8— §7View stats or leaderboard");
        sender.sendMessage("§a/spawn §8— §7Teleport to spawn");
        sender.sendMessage("§a/forfeit §8— §7Forfeit your current duel");
        sender.sendMessage("§a/ping [player] §8— §7Check ping");
        sender.sendMessage("§a/settings §8— §7Open settings menu");

        if (sender.hasPermission("duels.admin")) {
            sender.sendMessage("");
            sender.sendMessage("§c§lAdmin Commands:");
            sender.sendMessage("§c/setspawn §8— §7Set the spawn location");
            sender.sendMessage("§c/kit add <name> <item> §8— §7Create a kit from your inventory");
            sender.sendMessage("§c/kit remove <name> §8— §7Delete a kit");
            sender.sendMessage("§c/arena setfirst <name> §8— §7Set arena spawn 1");
            sender.sendMessage("§c/arena setsecond <name> §8— §7Set arena spawn 2");
            sender.sendMessage("§c/arena setcorner1 <name> §8— §7Set arena corner 1");
            sender.sendMessage("§c/arena setcorner2 <name> §8— §7Set arena corner 2");
            sender.sendMessage("§c/arena create <name> §8— §7Finalise arena & save snapshot");
            sender.sendMessage("§c/arena kit <kit> <arena> §8— §7Bind a kit to always use an arena");
            sender.sendMessage("§c/arena list §8— §7List all arenas");
            sender.sendMessage("§c/arena info <name> §8— §7Show arena details");
            sender.sendMessage("§c/arena delete <name> §8— §7Delete an arena");
            sender.sendMessage("§c/setkills/setdeaths/setwins/setlosses <player> <amount>");
            sender.sendMessage("§c/duels reload §8— §7Reload plugin config");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("duels.reload")) {
            if ("reload".startsWith(args[0].toLowerCase())) return List.of("reload");
        }
        return List.of();
    }

    private void reloadPlugin(CommandSender sender) {
        plugin.getConfigManager().loadAllConfigs();
        plugin.getArenaManager().loadArenas();
        plugin.getKitManager().loadKits();
        plugin.getPlayerManager().loadPlayerData();

        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            plugin.getScoreboardManager().updateScoreboard(player);
        }

        sender.sendMessage(plugin.getPrefix() + "§7Plugin configuration §a§lreloaded!");
        plugin.getLogger().info("Duels plugin reloaded by " + sender.getName());
    }
}
