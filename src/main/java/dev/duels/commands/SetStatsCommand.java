package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class SetStatsCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public SetStatsCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("duels.admin")) {
            sender.sendMessage(plugin.getPrefix() + "§cYou don't have permission!");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(plugin.getPrefix() + "§7Usage: /" + label + " <player> <amount>");
            return true;
        }

        String playerName = args[0];
        int amount;

        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getPrefix() + "§cInvalid amount!");
            return true;
        }

        UUID targetUUID = plugin.getPlayerManager().getUUIDFromName(playerName);
        if (targetUUID == null) {
            sender.sendMessage(plugin.getPrefix() + "§cPlayer not found!");
            return true;
        }

        String statType = command.getName().toLowerCase().replace("set", "");
        plugin.getPlayerManager().setStat(targetUUID, statType, amount);

        sender.sendMessage(plugin.getPrefix() + "§2" + statType.substring(0, 1).toUpperCase() +
                statType.substring(1) + " §aupdated!");

        // Scoreboard updaten falls online
        org.bukkit.entity.Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null) {
            plugin.getScoreboardManager().updateScoreboard(targetPlayer);
        }

        return true;
    }
}