package dev.duels.objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DuelSession {

    public enum MatchMode {
        BEST_OF,
        FIRST_TO;

        public static MatchMode fromString(String value) {
            if (value == null) return FIRST_TO;

            String normalized = value.trim()
                    .toUpperCase()
                    .replace('-', '_')
                    .replace(' ', '_');

            if (normalized.equals("BESTOF")) normalized = "BEST_OF";
            if (normalized.equals("FIRSTTO")) normalized = "FIRST_TO";

            try {
                return MatchMode.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                return FIRST_TO;
            }
        }
    }

    private final UUID player1;
    private final UUID player2;
    private final String kitName;
    private final String arenaName;
    private int timeLeft;
    private final MatchMode matchMode;
    private final int matchValue;
    private int winsP1;
    private int winsP2;
    private int round;
    private boolean roundStarting;

    public DuelSession(UUID player1, UUID player2, String kitName, String arenaName, int timeLeft, MatchMode matchMode, int matchValue) {
        this.player1 = player1;
        this.player2 = player2;
        this.kitName = kitName;
        this.arenaName = arenaName;
        this.timeLeft = timeLeft;
        this.matchMode = matchMode == null ? MatchMode.FIRST_TO : matchMode;
        this.matchValue = Math.max(1, matchValue);
        this.winsP1 = 0;
        this.winsP2 = 0;
        this.round = 1;
        this.roundStarting = false;
    }

    public UUID getPlayer1() { return player1; }
    public UUID getPlayer2() { return player2; }
    public String getKitName() { return kitName; }
    public String getArenaName() { return arenaName; }
    public int getTimeLeft() { return timeLeft; }
    public void setTimeLeft(int timeLeft) { this.timeLeft = timeLeft; }
    public MatchMode getMatchMode() { return matchMode; }
    public int getMatchValue() { return matchValue; }
    public int getBestOf() { return matchValue; }
    public int getWinsP1() { return winsP1; }
    public void setWinsP1(int winsP1) { this.winsP1 = winsP1; }
    public int getWinsP2() { return winsP2; }
    public void setWinsP2(int winsP2) { this.winsP2 = winsP2; }
    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }
    public boolean isRoundStarting() { return roundStarting; }
    public void setRoundStarting(boolean roundStarting) { this.roundStarting = roundStarting; }

    public UUID getOpponent(UUID player) {
        if (player1.equals(player)) return player2;
        if (player2.equals(player)) return player1;
        return null;
    }

    public String getOpponentName(UUID player) {
        UUID opponent = getOpponent(player);
        if (opponent == null) return "None";

        Player oppPlayer = Bukkit.getPlayer(opponent);
        return oppPlayer != null ? oppPlayer.getName() : "None";
    }

    public boolean contains(UUID player) {
        return player1.equals(player) || player2.equals(player);
    }

    public int requiredWins() {
        return matchMode == MatchMode.BEST_OF ? (matchValue / 2) + 1 : matchValue;
    }

    public String getMatchDescription() {
        return matchMode == MatchMode.BEST_OF ? "Best of " + matchValue : "First to " + matchValue;
    }

    public String getScoreString() {
        return winsP1 + "§7-§f" + winsP2;
    }
}
