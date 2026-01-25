package dev.duels.commands;

import dev.duels.DuelsPlugin;
import dev.duels.objects.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public StatsCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + "§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showPlayerStats(player, player.getUniqueId());
        } else {
            // Stats von anderem Spieler anzeigen
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                showPlayerStats(player, target.getUniqueId());
            } else {
                // Spieler offline, versuchen aus Config zu laden
                UUID targetUUID = plugin.getPlayerManager().getUUIDFromName(args[0]);
                if (targetUUID != null) {
                    showPlayerStats(player, targetUUID);
                } else {
                    player.sendMessage(plugin.getPrefix() + "§cPlayer not found!");
                }
            }
        }

        return true;
    }

    private void showPlayerStats(Player viewer, UUID targetUUID) {
        PlayerData data = plugin.getPlayerManager().getPlayerData(targetUUID);

        int kills = data.getKills();
        int deaths = data.getDeaths();
        int wins = data.getWins();
        int losses = data.getLosses();
        double kd = data.getKD();
        double winrate = data.getWinRate();

        String playerName = data.getName();
        if (playerName == null || playerName.equals("Unknown")) {
            Player online = Bukkit.getPlayer(targetUUID);
            if (online != null) {
                playerName = online.getName();
            }
        }

        viewer.sendMessage(plugin.getPrefix() + "§bStats §7for §a" + playerName);
        viewer.sendMessage("§2\uD83D\uDDE1 §7ᴋɪʟʟѕ §2" + kills);
        viewer.sendMessage("§c☠ §7ᴅᴇᴀᴛʜѕ §c" + deaths);
        viewer.sendMessage("§e❤ §7ᴋᴅ §e" + String.format("%.2f", kd));
        viewer.sendMessage("§a✔ §7ᴡɪɴѕ §a" + wins);
        viewer.sendMessage("§c✘ §7ʟᴏѕѕᴇѕ §c" + losses);
        viewer.sendMessage("§b🧪 §7ᴡɪɴ ʀᴀᴛᴇ §b" + String.format("%.1f%%", winrate));
    }
}