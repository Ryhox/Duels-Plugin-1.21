package dev.duels.managers;

import dev.duels.DuelsPlugin;
import dev.duels.objects.DuelSession;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, Objective> playerObjectives = new HashMap<>();

    public ScoreboardManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        boolean inDuel = plugin.getDuelManager().isInDuel(uuid);

        Scoreboard board = playerScoreboards.get(uuid);
        Objective obj = playerObjectives.get(uuid);

        String title = plugin.getConfigManager().getMainConfig().getString("scoreboard-title", "§3§l🪓 Duels");

        if (board == null || obj == null) {
            org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
            if (bukkitManager == null) return;

            board = bukkitManager.getNewScoreboard();

            if (obj != null) {
                obj.unregister();
            }

            obj = board.registerNewObjective("duels", "dummy", title);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            playerScoreboards.put(uuid, board);
            playerObjectives.put(uuid, obj);
            player.setScoreboard(board);
        } else {
            if (!obj.getDisplayName().equals(title)) {
                obj.setDisplayName(title);
            }
        }

        // Linien holen
        java.util.List<String> lines;
        if (inDuel) {
            lines = plugin.getConfigManager().getMainConfig().getStringList("duel-scoreboard-lines");
        } else {
            lines = plugin.getConfigManager().getMainConfig().getStringList("scoreboard-lines");
        }

        // Alte Einträge löschen
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        // Platzhalter ersetzen
        int score = lines.size();
        for (String line : lines) {
            line = replacePlaceholders(player, line);

            if (line.isEmpty() || line.equals(" ")) {
                String emptyLineId = getEmptyLineId(score);
                obj.getScore(emptyLineId).setScore(score);
            } else {
                obj.getScore(line).setScore(score);
            }
            score--;
        }
    }

    private String replacePlaceholders(Player player, String line) {
        UUID uuid = player.getUniqueId();

        // Spieler Stats
        int kills = plugin.getPlayerManager().getStat(uuid, "kills");
        int deaths = plugin.getPlayerManager().getStat(uuid, "deaths");
        int wins = plugin.getPlayerManager().getStat(uuid, "wins");
        int losses = plugin.getPlayerManager().getStat(uuid, "losses");
        double kd = deaths == 0 ? kills : (double) kills / deaths;
        String winrate = plugin.getPlayerManager().calculateWinrate(wins, losses);

        // Allgemeine Stats
        int online = Bukkit.getOnlinePlayers().size();
        int playing = plugin.getDuelManager().getActiveDuelCount() * 2;

        // Duel-spezifische Stats
        DuelSession session = plugin.getDuelManager().getDuelSession(uuid);
        String opponentName = "None";
        String mapName = plugin.getConfigManager().getMainConfig().getString("default-map", "§cDefault");
        int playerPing = player.getPing();
        int opponentPing = 0;
        int timeLeft = 0;

        if (session != null) {
            opponentName = session.getOpponentName(uuid);
            mapName = session.getArenaName();
            timeLeft = session.getTimeLeft();


            UUID opponentId = session.getOpponent(uuid);
            if (opponentId != null) {
                Player opponent = Bukkit.getPlayer(opponentId);
                if (opponent != null) {
                    opponentPing = opponent.getPing();
                }
            }
        }

        // Alle Platzhalter ersetzen
        return line
                .replace("%kills%", String.valueOf(kills))
                .replace("%deaths%", String.valueOf(deaths))
                .replace("%wins%", String.valueOf(wins))
                .replace("%losses%", String.valueOf(losses))
                .replace("%kd%", String.format("%.2f", kd))
                .replace("%winrate%", winrate)
                .replace("%online%", String.valueOf(online))
                .replace("%playing%", String.valueOf(playing))
                .replace("%opponent%", opponentName)
                .replace("%map%", mapName)
                .replace("%playerping%", playerPing + "ms")
                .replace("%opponentping%", opponentPing + "ms")
                .replace("%timeleft%", (timeLeft) + "s");
    }

    private String getEmptyLineId(int index) {
        return ChatColor.values()[index % ChatColor.values().length].toString() + ChatColor.RESET;
    }

    public void removeScoreboard(UUID uuid) {
        Objective obj = playerObjectives.remove(uuid);
        if (obj != null) {
            obj.unregister();
        }
        playerScoreboards.remove(uuid);
    }

    public void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }
}