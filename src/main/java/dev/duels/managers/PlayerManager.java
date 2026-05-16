package dev.duels.managers;

import dev.duels.DuelsPlugin;
import dev.duels.objects.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PlayerManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, PlayerData> playerData = new HashMap<>();
    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private final Map<UUID, Boolean> autoFly = new HashMap<>();

    public PlayerManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }


    public void loadPlayerData(UUID uuid) {
        if (playerData.containsKey(uuid) && autoFly.containsKey(uuid)) return;

        String key = uuid.toString();

        PlayerData data = playerData.computeIfAbsent(uuid, k -> new PlayerData(uuid));

        data.setKills(plugin.getConfigManager().getPlayersConfig().getInt(key + ".kills", 0));
        data.setDeaths(plugin.getConfigManager().getPlayersConfig().getInt(key + ".deaths", 0));
        data.setWins(plugin.getConfigManager().getPlayersConfig().getInt(key + ".wins", 0));
        data.setLosses(plugin.getConfigManager().getPlayersConfig().getInt(key + ".losses", 0));
        data.setName(plugin.getConfigManager().getPlayersConfig().getString(key + ".name", "Unknown"));

        boolean af = plugin.getConfigManager().getPlayersConfig().getBoolean(key + ".autofly", true);
        autoFly.put(uuid, af);
    }

    public void loadPlayerData() {
        playerData.clear();
        autoFly.clear();
        hiddenPlayers.clear();

        for (String key : plugin.getConfigManager().getPlayersConfig().getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                loadPlayerData(uuid); // nutzt deine Single-Loader Methode
            } catch (IllegalArgumentException ignored) {}
        }
    }


    public void savePlayerData(UUID uuid) {
        PlayerData data = playerData.get(uuid);
        if (data == null) return;

        String key = uuid.toString();
        plugin.getConfigManager().getPlayersConfig().set(key + ".kills", data.getKills());
        plugin.getConfigManager().getPlayersConfig().set(key + ".deaths", data.getDeaths());
        plugin.getConfigManager().getPlayersConfig().set(key + ".wins", data.getWins());
        plugin.getConfigManager().getPlayersConfig().set(key + ".losses", data.getLosses());
        plugin.getConfigManager().getPlayersConfig().set(key + ".name", data.getName());
        plugin.getConfigManager().getPlayersConfig().set(key + ".autofly", autoFly.getOrDefault(uuid, true));

        plugin.getConfigManager().savePlayersConfig();
    }

    public void saveAllData() {
        for (UUID uuid : playerData.keySet()) {
            savePlayerData(uuid);
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        loadPlayerData(uuid);
        return playerData.computeIfAbsent(uuid, k -> new PlayerData(uuid));
    }



    public void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getScoreboardManager().updateScoreboard(player);
        }
    }

    public void addStat(UUID uuid, String stat, int amount) {
        PlayerData data = getPlayerData(uuid);
        switch (stat.toLowerCase()) {
            case "kills":
                data.setKills(data.getKills() + amount);
                break;
            case "deaths":
                data.setDeaths(data.getDeaths() + amount);
                break;
            case "wins":
                data.setWins(data.getWins() + amount);
                break;
            case "losses":
                data.setLosses(data.getLosses() + amount);
                break;
        }
        savePlayerData(uuid);
    }

    public void setStat(UUID uuid, String stat, int value) {
        PlayerData data = getPlayerData(uuid);
        switch (stat.toLowerCase()) {
            case "kills":
                data.setKills(value);
                break;
            case "deaths":
                data.setDeaths(value);
                break;
            case "wins":
                data.setWins(value);
                break;
            case "losses":
                data.setLosses(value);
                break;
        }
        savePlayerData(uuid);
    }
public List<PlayerData> getAllPlayerDataSnapshot() {
    List<PlayerData> list = new ArrayList<>();

    for (String key : plugin.getConfigManager().getPlayersConfig().getKeys(false)) {
        if (!isValidUUID(key)) continue;

        UUID uuid;
        try {
            uuid = UUID.fromString(key);
        } catch (IllegalArgumentException ignored) {
            continue;
        }

        loadPlayerData(uuid);
        PlayerData data = getPlayerData(uuid);
        if (data != null) list.add(data);
    }
    return list;
}

    public void setupPlayerInventory(Player player) {
        player.getInventory().clear();

        // Slot 0: Challenge Sword
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setDisplayName("§aᴄʜᴀʟʟᴇɴɢᴇ");
        swordMeta.setLore(java.util.Arrays.asList(
                "§7Hit a §cPlayer §7to §achallenge §7them",
                "§7Right-click to §ajoin the §aqueue"
        ));
        sword.setItemMeta(swordMeta);
        player.getInventory().setItem(0, sword);

        // Queue Slot (wird durch refreshQueueSlotItem gesetzt)
        refreshQueueSlotItem(player);

        // Slot 1: Stats
        ItemStack stats = new ItemStack(Material.PAPER);
        ItemMeta statsMeta = stats.getItemMeta();
        statsMeta.setDisplayName("§bѕᴛᴀᴛѕ §7(ʀɪɢʜᴛᴄʟɪᴄᴋ)");
        statsMeta.setLore(java.util.Arrays.asList("§7See your stats or see the leaderboard"));
        stats.setItemMeta(statsMeta);
        player.getInventory().setItem(1, stats);

        // Slot 7: Visibility
        ItemStack visibility = new ItemStack(Material.GREEN_DYE);
        ItemMeta visibilityMeta = visibility.getItemMeta();
        visibilityMeta.setDisplayName("§aᴘʟᴀʏᴇʀ ᴠɪѕɪʙɪʟɪᴛʏ ᴏɴ §7(ʀɪɢʜᴛᴄʟɪᴄᴋ)");
        visibilityMeta.setLore(java.util.Arrays.asList("§7Change the Player visibility."));
        visibility.setItemMeta(visibilityMeta);
        player.getInventory().setItem(7, visibility);

        // Slot 8: Settings
        ItemStack settings = new ItemStack(Material.REPEATER);
        ItemMeta settingsMeta = settings.getItemMeta();
        settingsMeta.setDisplayName("§cѕᴇᴛᴛɪɴɢѕ §7(ʀɪɢʜᴛᴄʟɪᴄᴋ)");
        settingsMeta.setLore(java.util.Arrays.asList("§7All kind of settings"));
        settings.setItemMeta(settingsMeta);
        player.getInventory().setItem(8, settings);

        player.updateInventory();
    }
    public void applyVisibility(Player viewer) {
        if (viewer == null) return;

        boolean hidden = isHidden(viewer.getUniqueId());

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(viewer)) continue;

            if (hidden) {
                viewer.hidePlayer(plugin, other);
            } else {
                viewer.showPlayer(plugin, other);
            }
        }
    }


    public void handleJoinVisibility(Player joiner) {
        if (joiner == null) return;

        // Apply joiner's own preference
        applyVisibility(joiner);

        // If other players have visibility OFF, they should hide the joiner too
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(joiner)) continue;
            if (isHidden(viewer.getUniqueId())) {
                viewer.hidePlayer(plugin, joiner);
            }
        }
    }

    public void refreshQueueSlotItem(Player player) {
        UUID uuid = player.getUniqueId();

        if (plugin.getDuelManager().isInDuel(uuid)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        // Slot 4: Queue Item
        if (plugin.getQueueManager().isInQueue(uuid)) {
            // Leave Queue Item
            ItemStack leaveQueue = new ItemStack(Material.BARRIER);
            ItemMeta meta = leaveQueue.getItemMeta();
            meta.setDisplayName("§cʟᴇᴀᴠᴇ ǫᴜᴇᴜᴇ §7(ʀɪɢʜᴛᴄʟɪᴄᴋ)");
            meta.setLore(java.util.Arrays.asList("§7Click to leave your current queue."));
            leaveQueue.setItemMeta(meta);
            player.getInventory().setItem(4, leaveQueue);
        } else {
            // Join Last Queue Item
            ItemStack joinQueue = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta headMeta = (SkullMeta) joinQueue.getItemMeta();
            headMeta.setDisplayName("§aᴊᴏɪɴ ʟᴀѕᴛ ǫᴜᴇᴜᴇ ᴀɢᴀɪɴ §7(ʀɪɢʜᴛᴄʟɪᴄᴋ)");
            headMeta.setLore(java.util.Arrays.asList("§7Here you can join the same Queue again."));
            headMeta.setOwningPlayer(player);
            joinQueue.setItemMeta(headMeta);
            player.getInventory().setItem(4, joinQueue);
        }

        player.updateInventory();
    }

    public void teleportToSpawn(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) return;
        Location spawn = plugin.getArenaManager().getSpawnLocation();
        if (spawn == null) {
            player.sendMessage(plugin.getPrefix() + "§cSpawn has not been set yet!");
            return;
        }

        player.teleport(spawn);

        if (!plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            forceLobbyState(player);
            setupPlayerInventory(player);
            refreshQueueSlotItem(player);
            applyLobbyFly(player);
        }

        plugin.getScoreboardManager().updateScoreboard(player);
    }

    // Füge diese Methoden zur PlayerManager Klasse hinzu:

    public void forceLobbyState(Player player) {
        if (player == null) return;

        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setRemainingAir(player.getMaximumAir());
        player.setFreezeTicks(0);
        player.setAbsorptionAmount(0.0);

        // Potion Effects entfernen
        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType()));

        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
    }


    public void applyLobbyFly(Player player) {
        if (player == null) return;

        if (player.getGameMode() == GameMode.CREATIVE) return;

        if (!isInLobby(player)) {
            player.setFlying(false);
            player.setAllowFlight(false);
            return;
        }

        if (!player.hasPermission("duels.fly")) {
            player.setFlying(false);
            player.setAllowFlight(false);
            return;
        }

        if (!getAutoFly(player.getUniqueId())) {
            player.setFlying(false);
            player.setAllowFlight(false);
            return;
        }

        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public boolean isInLobby(Player player) {
        if (player == null) return false;
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) return false;

        Location spawn = plugin.getArenaManager().getSpawnLocation();
        if (spawn != null && player.getWorld() != null) {
            return player.getWorld().getUID().equals(spawn.getWorld().getUID());
        }
        return true;
    }

    // --- DUEL VISIBILITY ---


    public void applyDuelVisibility(Player p1, Player p2) {
        if (p1 == null || p2 == null) return;

        // Duelists: hide everyone except each other
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p1) || other.equals(p2)) continue;
            p1.hidePlayer(plugin, other);
            p2.hidePlayer(plugin, other);
        }

        // Ensure duelists see each other
        p1.showPlayer(plugin, p2);
        p2.showPlayer(plugin, p1);

        // Everyone else: hide BOTH duelists
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(p1) || viewer.equals(p2)) continue;
            viewer.hidePlayer(plugin, p1);
            viewer.hidePlayer(plugin, p2);
        }
    }


    public void restoreAllVisibility() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            // First: show everyone to reset hard-hide from duel
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(other)) continue;
                viewer.showPlayer(plugin, other);
            }

            // Then apply viewer's preference (hidden or not)
            applyVisibility(viewer);
            updatePlayerVisibility(viewer);
        }
    }


    public void enforceDuelPrivacyForJoin(Player joiner) {
        if (joiner == null) return;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(joiner)) continue;

            boolean otherInDuel = plugin.getDuelManager().isInDuel(other.getUniqueId());

            // If other is in a duel: joiner can't see them AND they can't see joiner
            if (otherInDuel) {
                joiner.hidePlayer(plugin, other);
                other.hidePlayer(plugin, joiner);
            }
        }
    }



    public void updatePlayerVisibility(Player player) {
        boolean hidden = isHidden(player.getUniqueId());
        ItemStack visibilityItem = new ItemStack(hidden ? Material.RED_DYE : Material.GREEN_DYE);
        ItemMeta meta = visibilityItem.getItemMeta();

        if (hidden) {
            meta.setDisplayName("§cᴘʟᴀʏᴇʀ ᴠɪѕɪʙɪʟɪᴛʏ ᴏꜰꜰ §7(ʀɪɢʜᴛᴄʟɪᴄᴋ)");
            meta.setLore(java.util.Arrays.asList("§7Change the Player visibility", "§c✗ Players are invisible"));
        } else {
            meta.setDisplayName("§aᴘʟᴀʏᴇʀ ᴠɪѕɪʙɪʟɪᴛʏ ᴏɴ §7(ʀɪɢʜᴛᴄʟɪᴄᴋ)");
            meta.setLore(java.util.Arrays.asList("§7Change the Player visibility", "§a✓ Players are visible"));
        }

        visibilityItem.setItemMeta(meta);
        player.getInventory().setItem(7, visibilityItem);
        player.updateInventory();
    }

    public UUID getUUIDFromName(String playerName) {
        // Online Spieler zuerst checken
        Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        // Aus Config suchen
        for (String key : plugin.getConfigManager().getPlayersConfig().getKeys(false)) {
            if (isValidUUID(key)) {
                String storedName = plugin.getConfigManager().getPlayersConfig().getString(key + ".name");
                if (storedName != null && storedName.equalsIgnoreCase(playerName)) {
                    try {
                        return UUID.fromString(key);
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                }
            }
        }

        return null;
    }
    public int getStat(UUID uuid, String stat) {
        PlayerData data = getPlayerData(uuid);
        switch (stat.toLowerCase()) {
            case "kills": return data.getKills();
            case "deaths": return data.getDeaths();
            case "wins": return data.getWins();
            case "losses": return data.getLosses();
            default: return 0;
        }
    }

    public String calculateWinrate(int wins, int losses) {
        int total = wins + losses;
        if (total == 0) return "0%";
        double winrate = ((double) wins / total) * 100;
        return String.format("%.1f%%", winrate);
    }

    public boolean isHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }

    public void toggleVisibility(UUID uuid) {
        if (hiddenPlayers.contains(uuid)) {
            hiddenPlayers.remove(uuid);
        } else {
            hiddenPlayers.add(uuid);
        }
    }

    public boolean getAutoFly(UUID uuid) {
        return autoFly.getOrDefault(uuid, true);
    }

    public void setAutoFly(UUID uuid, boolean value) {
        autoFly.put(uuid, value);
        savePlayerData(uuid);
    }

    private boolean isValidUUID(String string) {
        try {
            UUID.fromString(string);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}