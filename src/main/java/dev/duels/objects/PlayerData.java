package dev.duels.objects;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int wins;
    private int losses;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.kills = 0;
        this.deaths = 0;
        this.wins = 0;
        this.losses = 0;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public double getKD() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public double getWinRate() {
        int total = wins + losses;
        return total == 0 ? 0.0 : ((double) wins / total) * 100.0;
    }
}