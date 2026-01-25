package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public SetSpawnCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("duels.setspawn")) {
            player.sendMessage(plugin.getPrefix() + "§cYou don't have permission!");
            return true;
        }

        plugin.getArenaManager().setSpawnLocation(player.getLocation());
        player.sendMessage(plugin.getPrefix() + "§a§lSpawn§7 has been §aset!");
        return true;
    }
}