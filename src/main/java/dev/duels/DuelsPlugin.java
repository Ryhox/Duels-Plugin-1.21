package dev.duels;

import dev.duels.commands.*;
import dev.duels.guis.GUIManager;
import dev.duels.listeners.*;
import dev.duels.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DuelsPlugin extends JavaPlugin {

    private static DuelsPlugin instance;
    private ConfigManager configManager;
    private DuelManager duelManager;
    private ArenaManager arenaManager;
    private QueueManager queueManager;
    private KitManager kitManager;
    private PlayerManager playerManager;
    private ScoreboardManager scoreboardManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        // Manager initialisieren
        configManager = new ConfigManager(this);
        arenaManager = new ArenaManager(this);
        kitManager = new KitManager(this);
        playerManager = new PlayerManager(this);
        scoreboardManager = new ScoreboardManager(this);
        queueManager = new QueueManager(this);
        duelManager = new DuelManager(this);
        guiManager = new GUIManager(this);

        // Konfigurationen laden
        configManager.loadAllConfigs();
        configManager.reloadPlayersConfig();
        kitManager.loadCustomLayoutsFromFile();

        Bukkit.getScheduler().runTaskLater(this, () -> arenaManager.loadArenas(), 40L);
        kitManager.loadKits();
        playerManager.loadPlayerData();





        // Events registrieren
        registerListeners();

        // Commands registrieren
        registerCommands();

        // Tasks starten
        startTasks();



        String CYAN = "\u001B[36m";
        String GREEN = "\u001B[32m";
        String GRAY = "\u001B[90m";
        String BOLD = "\u001B[1m";
        String RESET = "\u001B[0m";

        Bukkit.getLogger().info(GRAY + BOLD + "╔════════════════════════════╗" + RESET);
        Bukkit.getLogger().info(GRAY + BOLD + "║       " + CYAN + "Duels Plugin" + GRAY + "         ║" + RESET);
        Bukkit.getLogger().info(GRAY + BOLD + "║    " + GREEN + "Successfully Enabled" + GRAY + "    ║" + RESET);
        Bukkit.getLogger().info(GRAY + BOLD + "╚════════════════════════════╝" + RESET);

    }

    @Override
    public void onDisable() {
        // Alles sauber herunterfahren
        duelManager.cleanupAll();
        arenaManager.cleanup();
        queueManager.cleanup();
        playerManager.saveAllData();
        configManager.saveAllConfigs();

        getLogger().info("Duels plugin disabled!");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DuelListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ArenaListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HotbarLockListener(this), this);

    }

    private void registerCommands() {
        getCommand("duels").setExecutor(new MainCommand(this));
        getCommand("duel").setExecutor(new DuelCommand(this));
        getCommand("queue").setExecutor(new QueueCommand(this));
        getCommand("kits").setExecutor(new KitsCommand(this));
        getCommand("previewkit").setExecutor(new PreviewKitCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("settings").setExecutor(new SettingsCommand(this));
        getCommand("arena").setExecutor(new ArenaCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("setkills").setExecutor(new SetStatsCommand(this));
        getCommand("setdeaths").setExecutor(new SetStatsCommand(this));
        getCommand("setwins").setExecutor(new SetStatsCommand(this));
        getCommand("setlosses").setExecutor(new SetStatsCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("ping").setExecutor(new PingCommand(this));
        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("accept").setExecutor(new AcceptCommand(this));
    }

    private void startTasks() {
        // Scoreboard Update Task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            playerManager.updateAllScoreboards();
            duelManager.updateDuelTimers();
            queueManager.checkQueueMatches();
        }, 0L, 20L);

        // GUI Update Task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            guiManager.updateOpenGUIs();
        }, 0L, 20L);
    }

    public static DuelsPlugin getInstance() {
        return instance;
    }

    // Getter für alle Manager
    public ConfigManager getConfigManager() { return configManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public KitManager getKitManager() { return kitManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public GUIManager getGuiManager() { return guiManager; }

    // Konstanten
    public String getPrefix() {
        return "§9§lᴅᴜᴇʟѕ §8| §7";
    }
}