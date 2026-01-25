package dev.duels.commands;

import dev.duels.DuelsPlugin;
import dev.duels.managers.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public DuelCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + "§cOnly players can use this command!");
            return true;
        }


        if (args.length < 1) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: §a/duel <player> §7| §a/duel accept §7| §a/duel deny");
            return true;
        }


        // Subcommands: accept/deny
        if (args[0].equalsIgnoreCase("accept")) {
            plugin.getDuelManager().acceptDuelRequest(player);
            return true;
        }


        if (args[0].equalsIgnoreCase("deny")) {
            plugin.getDuelManager().denyDuelRequest(player);
            return true;
        }


        // Normal: /duel <player>
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§cYou are already in a duel!");
            return true;
        }


        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getPrefix() + "§cPlayer not found or offline!");
            return true;
        }


        if (target.equals(player)) {
            player.sendMessage(plugin.getPrefix() + "§cYou cannot duel yourself!");
            return true;
        }


        if (plugin.getDuelManager().isInDuel(target.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + "§cThat player is already in a duel!");
            return true;
        }


        // Kit Selection GUI öffnen
        plugin.getGuiManager().openDuelGUI(player, target);
        return true;
    }
}