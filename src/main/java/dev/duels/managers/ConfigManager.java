package dev.duels.managers;

import dev.duels.DuelsPlugin;
import dev.duels.objects.DuelSession;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final DuelsPlugin plugin;
    private FileConfiguration mainConfig;
    private FileConfiguration playersConfig;
    private FileConfiguration kitsConfig;
    private FileConfiguration arenaConfig;

    private File playersFile;
    private File kitsFile;
    private File arenaFile;

    public ConfigManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAllConfigs() {
        // Main config - reload from disk so /duels reload picks up file changes
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();

        // Players config
        playersFile = new File(plugin.getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            createDefaultFile(playersFile, "players.yml");
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);

        // Kits config
        kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        if (!kitsFile.exists()) {
            createDefaultFile(kitsFile, "kits.yml");
        }
        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);

        // Arena config
        arenaFile = new File(plugin.getDataFolder(), "arena.yml");
        if (!arenaFile.exists()) {
            createDefaultFile(arenaFile, "arena.yml");
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);

        // Fill in any missing defaults
        setDefaults();
    }

    public void reloadPlayersConfig() {
        if (playersFile == null) {
            playersFile = new File(plugin.getDataFolder(), "players.yml");
        }
        if (!playersFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                playersFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().severe("Could not create players.yml: " + e.getMessage());
            }
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    private void createDefaultFile(File file, String resourceName) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            InputStream in = plugin.getResource(resourceName);
            if (in != null) {
                Files.copy(in, file.toPath());
            } else {
                file.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create " + resourceName + "!");
        }
    }

    private void setDefaults() {
        // Check each key individually so new keys are added even on existing configs
        boolean changed = false;

        if (!mainConfig.contains("scoreboard-title")) {
            mainConfig.set("scoreboard-title", "§3§l🪓 Duels");
            changed = true;
        }
        if (!mainConfig.contains("scoreboard-lines")) {
            mainConfig.set("scoreboard-lines", java.util.Arrays.asList(
                    "",
                    "§a☻ §7ᴏɴʟɪɴᴇ §a%online%",
                    "§6🏹 §7ɪɴ ᴅᴜᴇʟѕ §6%playing%",
                    "",
                    "§2🗡 §7ᴋɪʟʟѕ §2%kills%",
                    "§c☠ §7ᴅᴇᴀᴛʜѕ §c%deaths%",
                    "§e❤ §7ᴋᴅ §e%kd%",
                    "",
                    "§a✔ §7ᴡɪɴѕ §a%wins%",
                    "§4✘ §7ʟᴏѕѕᴇѕ §4%losses%",
                    "§b🧪 §7ᴡɪɴ ʀᴀᴛᴇ §b%winrate%",
                    ""
            ));
            changed = true;
        }
        if (!mainConfig.contains("duel-scoreboard-lines")) {
            mainConfig.set("duel-scoreboard-lines", java.util.Arrays.asList(
                    "",
                    "§a☻ §7ᴏɴʟɪɴᴇ §a%online%",
                    "§6🏹 §7ɪɴ ᴅᴜᴇʟѕ §6%playing%",
                    "",
                    "§4🔥 §7ᴏᴘᴘᴏɴᴇɴᴛ §4%opponent%",
                    "§b🗺 §7ᴍᴀᴘ §b%map%",
                    "",
                    "§7🛜ʏᴏᴜʀ ᴘɪɴɢ §a%playerping%",
                    "§7🛜ᴏᴘᴘᴏɴᴇɴᴛѕ ᴘɪɴɢ §c%opponentping%",
                    "",
                    "§e🧪 §7ᴛɪᴍᴇ ʟᴇꜰᴛ §e%timeleft%",
                    ""
            ));
            changed = true;
        }
        if (!mainConfig.contains("duel-time")) {
            mainConfig.set("duel-time", 180);
            changed = true;
        }
        if (!mainConfig.contains("request-timeout")) {
            mainConfig.set("request-timeout", 30);
            changed = true;
        }
        if (!mainConfig.contains("default-map")) {
            mainConfig.set("default-map", "§cᴅᴜᴇʟѕ ᴍᴀᴘ");
            changed = true;
        }
        if (!mainConfig.contains("match-mode")) {
            mainConfig.set("match-mode", "first-to");
            changed = true;
        }
        if (!mainConfig.contains("default-rounds")) {
            mainConfig.set("default-rounds", mainConfig.getInt("default-bestof", 3));
            changed = true;
        }
        if (!mainConfig.contains("round-options")) {
            List<Integer> legacyOptions = mainConfig.getIntegerList("bestof-options");
            mainConfig.set("round-options", legacyOptions.isEmpty() ? java.util.Arrays.asList(1, 2, 3, 5, 10) : legacyOptions);
            changed = true;
        }
        if (!mainConfig.contains("min-rounds")) {
            mainConfig.set("min-rounds", 1);
            changed = true;
        }
        if (!mainConfig.contains("max-rounds")) {
            mainConfig.set("max-rounds", 25);
            changed = true;
        }
        if (!mainConfig.contains("bound-arena-random-fallback")) {
            mainConfig.set("bound-arena-random-fallback", false);
            changed = true;
        }

        if (changed) plugin.saveConfig();
    }

    public DuelSession.MatchMode getMatchMode() {
        return DuelSession.MatchMode.fromString(mainConfig.getString("match-mode", "first-to"));
    }

    public int getDefaultMatchValue() {
        int value;
        if (mainConfig.contains("default-rounds")) {
            value = mainConfig.getInt("default-rounds", 3);
        } else {
            value = mainConfig.getInt("default-bestof", 3);
        }
        return clampMatchValue(value);
    }

    public List<Integer> getMatchOptions() {
        List<Integer> configured = mainConfig.getIntegerList("round-options");
        if (configured == null || configured.isEmpty()) {
            configured = mainConfig.getIntegerList("bestof-options");
        }

        Set<Integer> cleaned = new LinkedHashSet<>();
        if (configured != null) {
            for (Integer value : configured) {
                if (value == null) continue;
                cleaned.add(clampMatchValue(value));
            }
        }

        if (cleaned.isEmpty()) {
            cleaned.add(getDefaultMatchValue());
        }

        return new ArrayList<>(cleaned);
    }

    public int getMinMatchValue() {
        return Math.max(1, mainConfig.getInt("min-rounds", 1));
    }

    public int getMaxMatchValue() {
        return Math.max(getMinMatchValue(), mainConfig.getInt("max-rounds", 25));
    }

    public int clampMatchValue(int value) {
        return Math.max(getMinMatchValue(), Math.min(getMaxMatchValue(), value));
    }

    public String getMatchDescription(int value) {
        int clamped = clampMatchValue(value);
        return getMatchMode() == DuelSession.MatchMode.BEST_OF ? "Best of " + clamped : "First to " + clamped;
    }

    public boolean allowBoundArenaRandomFallback() {
        return mainConfig.getBoolean("bound-arena-random-fallback", false);
    }

    public void saveAllConfigs() {
        plugin.saveConfig();
        savePlayersConfig();
        saveKitsConfig();
        saveArenaConfig();
    }

    public void savePlayersConfig() {
        FileConfiguration snapshot = playersConfig;
        File file = playersFile;
        if (!plugin.isEnabled()) {
            savePlayersConfigNow(snapshot, file);
            return;
        }

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            savePlayersConfigNow(snapshot, file);
        });
    }

    private void savePlayersConfigNow(FileConfiguration snapshot, File file) {
        try {
            snapshot.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save players.yml: " + e.getMessage());
        }
    }

    public void saveKitsConfig() {
        try {
            kitsConfig.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save kits config!");
        }
    }

    public void saveArenaConfig() {
        try {
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save arena config!");
        }
    }

    // Getter
    public FileConfiguration getMainConfig() { return mainConfig; }
    public FileConfiguration getPlayersConfig() { return playersConfig; }
    public FileConfiguration getKitsConfig() { return kitsConfig; }
    public FileConfiguration getArenaConfig() { return arenaConfig; }
}
