package dev.duels.commands;

import dev.duels.DuelsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PreviewKitCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public PreviewKitCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /previewkit <kitname>");
            player.sendMessage(plugin.getPrefix() + "§7Use /kits to see available kits");
            return true;
        }

        String kitName = args[0];
        plugin.getKitManager().giveKitPreview(player, kitName);
        return true;
    }
}