package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public SpawnCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Check if player is in a duel
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            // Handle as forfeit
            plugin.getDuelManager().handleForfeit(player);
            return true;
        }

        // Normal spawn teleport
        plugin.getPlayerManager().teleportToSpawn(player);
        return true;
    }
}