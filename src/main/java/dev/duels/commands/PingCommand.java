package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PingCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public PingCommand(DuelsPlugin plugin) {
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
            // Eigenen Ping anzeigen
            int ping = getPlayerPing(player);
            player.sendMessage(plugin.getPrefix() + "§7Your ping: §a" + ping + "ms");
        } else {
            // Ping von anderem Spieler anzeigen
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(plugin.getPrefix() + "§cPlayer not found!");
                return true;
            }

            int targetPing = getPlayerPing(target);
            player.sendMessage(plugin.getPrefix() + "§7" + target.getName() + "'s ping: §a" + targetPing + "ms");
        }

        return true;
    }

    private int getPlayerPing(Player player) {
        return player.getPing();
    }
}