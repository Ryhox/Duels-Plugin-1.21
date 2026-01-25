package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MainCommand implements CommandExecutor {

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

        // Hilfe anzeigen
        sender.sendMessage(plugin.getPrefix() + "§aDuels Plugin by Ryhox.");
        sender.sendMessage("§3Commands:");
        sender.sendMessage("§c/duels reload §8- §7Reload the plugin configuration");
        sender.sendMessage("§c/spawn §8- §7Teleport to spawn");
        sender.sendMessage("§c/stats [player] §8- §7Check stats");
        sender.sendMessage("§c/duel <player> §8- §7Challenge a player to a duel");
        sender.sendMessage("§c/kits §8- §7View available kits");
        sender.sendMessage("§c/accept §8- §7Accept a pending duel request");
        sender.sendMessage("§c/ping <player> §8- §7Check ping");
        sender.sendMessage("§c/previewkit <kitname> §8- §7Preview a kit in your inventory");
        sender.sendMessage("§c/queue join <kit> §8- §7Join a queue");
        sender.sendMessage("§c/queue gui §8- §7Open the kit GUI");
        sender.sendMessage("§c/queue leave §8- §7Leave a queue");

        if (sender.hasPermission("duels.admin")) {
            sender.sendMessage("§c/setspawn §8- §7Set spawn location");
            sender.sendMessage("§c/setkills <player> <amount> §8- §7Set player kills");
            sender.sendMessage("§c/setdeaths <player> <amount> §8- §7Set player deaths");
            sender.sendMessage("§c/setwins <player> <amount> §8- §7Set player wins");
            sender.sendMessage("§c/setlosses <player> <amount> §8- §7Set player losses");
            sender.sendMessage("§c/addkit <name> <preview_item> §8- §7Create a new kit");
            sender.sendMessage("§c/arenacreate §8- §7Create and manage arenas");
        }

        return true;
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