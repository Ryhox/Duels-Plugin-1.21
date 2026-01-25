package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public FlyCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("duels.fly")) {
            player.sendMessage(plugin.getPrefix() + "§cYou don't have permission!");
            return true;
        }

        if (!plugin.getPlayerManager().isInLobby(player)) {
            player.sendMessage(plugin.getPrefix() + "§cYou can only use /fly in the lobby!");
            return true;
        }

        boolean currentlyFlying = player.isFlying() || player.getAllowFlight();

        if (currentlyFlying) {
            // Fly ausschalten
            player.setFlying(false);
            player.setAllowFlight(false);
            plugin.getPlayerManager().setAutoFly(player.getUniqueId(), false);
            player.sendMessage(plugin.getPrefix() + "§7Fly: §cOFF §8(autofly disabled)");
        } else {
            // Fly einschalten
            player.setAllowFlight(true);
            player.setFlying(true);
            plugin.getPlayerManager().setAutoFly(player.getUniqueId(), true);
            player.sendMessage(plugin.getPrefix() + "§7Fly: §aON §8(autofly enabled)");
        }

        return true;
    }
}