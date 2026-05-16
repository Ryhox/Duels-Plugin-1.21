package dev.duels.managers;

import dev.duels.DuelsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

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
        if (!mainConfig.contains("default-bestof")) {
            mainConfig.set("default-bestof", 3);
            changed = true;
        }
        if (!mainConfig.contains("bestof-options")) {
            mainConfig.set("bestof-options", java.util.Arrays.asList(1, 3, 5, 10));
            changed = true;
        }

        if (changed) plugin.saveConfig();
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
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                snapshot.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Could not save players.yml: " + e.getMessage());
            }
        });
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
