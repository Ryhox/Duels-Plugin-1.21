package dev.duels.commands;

import dev.duels.DuelsPlugin;
import dev.duels.objects.DuelRequest;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcceptCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public AcceptCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        DuelRequest request = plugin.getDuelManager().getDuelRequest(player.getUniqueId());

        if (request == null) {
            player.sendMessage(plugin.getPrefix() + "§cYou don't have any pending duel requests!");
            return true;
        }

        int timeout = plugin.getConfigManager().getMainConfig().getInt("request-timeout", 30);
        if (request.isExpired(timeout)) {
            plugin.getDuelManager().removeDuelRequest(player.getUniqueId());
            player.sendMessage(plugin.getPrefix() + "§cThis duel request has expired!");
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(request.getSender());
        if (senderPlayer == null || !senderPlayer.isOnline()) {
            player.sendMessage(plugin.getPrefix() + "§cThe player who sent the request is no longer online!");
            plugin.getDuelManager().removeDuelRequest(player.getUniqueId());
            return true;
        }

        if (plugin.getDuelManager().isInDuel(senderPlayer.getUniqueId()) ||
                plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§cOne of you is already in a duel!");
            plugin.getDuelManager().removeDuelRequest(player.getUniqueId());
            return true;
        }

        // Duel starten
        plugin.getDuelManager().startDuel(request);
        plugin.getDuelManager().removeDuelRequest(player.getUniqueId());

        return true;
    }
}