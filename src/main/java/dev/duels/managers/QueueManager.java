package dev.duels.managers;

import dev.duels.DuelsPlugin;
import dev.duels.objects.DuelRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class QueueManager {

    private final DuelsPlugin plugin;
    private final Map<String, Deque<UUID>> kitQueues = new HashMap<>();
    private final Map<UUID, String> lastQueueKit = new HashMap<>();

    public QueueManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void joinQueue(Player player, String kitName) {
        if (!plugin.getKitManager().kitExists(kitName)) {
            player.sendMessage(plugin.getPrefix() + "§cKit not found: " + kitName);
            return;
        }

        UUID uuid = player.getUniqueId();

        // Aus allen Queues entfernen
        leaveAllQueues(uuid);

        // In neue Queue einreihen
        Deque<UUID> queue = kitQueues.computeIfAbsent(kitName, k -> new ArrayDeque<>());
        if (!queue.contains(uuid)) {
            queue.addLast(uuid);
            lastQueueKit.put(uuid, kitName);
            String kitDisplay = plugin.getKitManager().getKitDisplayName(kitName);
            player.sendMessage(plugin.getPrefix() + "§aQueued §7for kit §r" + kitDisplay + "§7.");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }

        // Queue-GUI aktualisieren
        plugin.getGuiManager().refreshQueueGUIs();
    }

    public void leaveQueue(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasInQueue = leaveAllQueues(uuid);

        if (wasInQueue) {
            player.sendMessage(plugin.getPrefix() + "§cLeft §7queue.");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 0.8f);
        }

        plugin.getGuiManager().refreshQueueGUIs();
    }

    public boolean leaveAllQueues(UUID uuid) {
        boolean wasInQueue = false;
        for (Deque<UUID> queue : kitQueues.values()) {
            if (queue.remove(uuid)) {
                wasInQueue = true;
            }
        }
        return wasInQueue;
    }

    public void checkQueueMatches() {
        for (Map.Entry<String, Deque<UUID>> entry : kitQueues.entrySet()) {
            String kitName = entry.getKey();
            Deque<UUID> queue = entry.getValue();

            // Offline Spieler entfernen
            queue.removeIf(id -> Bukkit.getPlayer(id) == null);

            if (queue.size() >= 2) {
                // Match starten
                UUID p1 = queue.pollFirst();
                UUID p2 = queue.pollFirst();

                Player player1 = Bukkit.getPlayer(p1);
                Player player2 = Bukkit.getPlayer(p2);

                if (player1 != null && player2 != null) {
                    startQueueMatch(player1, player2, kitName);
                } else {
                    // Zurück in Queue
                    if (player1 != null) joinQueue(player1, kitName);
                    if (player2 != null) joinQueue(player2, kitName);
                }
            }
        }
    }

    private void startQueueMatch(Player player1, Player player2, String kitName) {
        // Arena holen
        dev.duels.objects.Arena arena = plugin.getArenaManager().getRandomAvailableArena();
        if (arena == null) {
            player1.sendMessage(plugin.getPrefix() + "§cNo available arenas!");
            player2.sendMessage(plugin.getPrefix() + "§cNo available arenas!");
            joinQueue(player1, kitName);
            joinQueue(player2, kitName);
            return;
        }

        // DuelRequest erstellen
        int bestOf = plugin.getConfigManager().getMainConfig().getInt("default-bestof", 3);
        DuelRequest request = new DuelRequest(
                player1.getUniqueId(),
                player2.getUniqueId(),
                kitName,
                arena.getName(),
                bestOf
        );

        // Duel starten
        plugin.getDuelManager().startDuel(request);
    }

    public boolean isInQueue(UUID uuid) {
        for (Deque<UUID> queue : kitQueues.values()) {
            if (queue.contains(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInQueue(UUID uuid, String kitName) {
        Deque<UUID> queue = kitQueues.get(kitName);
        return queue != null && queue.contains(uuid);
    }

    public int getQueueSize(String kitName) {
        Deque<UUID> queue = kitQueues.get(kitName);
        return queue != null ? queue.size() : 0;
    }

    public int getPlayingCount(String kitName) {
        int count = 0;

        // Verwende getAllSessions() statt getActiveDuelCount()
        for (dev.duels.objects.DuelSession session : plugin.getDuelManager().getAllSessions()) {
            if (session.getKitName().equals(kitName)) {
                count += 2; // beide Spieler
            }
        }
        return count;
    }

    public String getLastQueueKit(UUID uuid) {
        return lastQueueKit.get(uuid);
    }

    public void setLastQueueKit(UUID uuid, String kitName) {
        lastQueueKit.put(uuid, kitName);
    }

    public void cleanup() {
        kitQueues.clear();
        lastQueueKit.clear();
    }
}