package dev.duels.managers;

import dev.duels.DuelsPlugin;
import dev.duels.objects.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

public class DuelManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, DuelSession> activeDuels = new HashMap<>();
    private final Map<UUID, DuelRequest> duelRequests = new HashMap<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, Location> pendingRoundRespawn = new HashMap<>();
    private final Set<UUID> roundDead = new HashSet<>();
    private final Map<PairKey, AutoSelect> autoSelect = new HashMap<>();
    private final Map<UUID, Long> lastRequestMs = new HashMap<>();
    private static final long REQUEST_COOLDOWN_MS = 10_000; // 10 Sekunden

    private final Map<UUID, PlayerState> savedStates = new HashMap<>();


    private static class PlayerState {
        final org.bukkit.inventory.ItemStack[] contents;
        final org.bukkit.inventory.ItemStack[] armor;
        final float exp;
        final int level;
        final int food;
        final float saturation;
        final double health;
        final org.bukkit.GameMode gameMode;
        final Location location;

        PlayerState(Player p) {
            this.contents = p.getInventory().getContents().clone();
            this.armor = p.getInventory().getArmorContents().clone();
            this.exp = p.getExp();
            this.level = p.getLevel();
            this.food = p.getFoodLevel();
            this.saturation = p.getSaturation();
            this.health = p.getHealth();
            this.gameMode = p.getGameMode();
            this.location = p.getLocation().clone();
        }

        void restore(Player p) {
            p.getInventory().clear();
            p.getInventory().setContents(contents);
            p.getInventory().setArmorContents(armor);
            p.setExp(exp);
            p.setLevel(level);
            p.setFoodLevel(food);
            p.setSaturation(saturation);
            p.setGameMode(gameMode);

            // Effekte weg
            for (PotionEffect e : p.getActivePotionEffects()) {
                p.removePotionEffect(e.getType());
            }

            p.setHealth(Math.min(health, p.getMaxHealth()));

            p.updateInventory();
        }
    }

    private void teleportToSpawnSafe(Player p) {
        if (p == null || !p.isOnline()) return;

        Location spawn = plugin.getArenaManager().getSpawnLocation();
        if (spawn != null) {
            p.teleport(spawn);
            return;
        }

        // Fallback: World Spawn
        Location worldSpawn = p.getWorld().getSpawnLocation();
        if (worldSpawn != null) p.teleport(worldSpawn);
    }

    private static final long AUTOSELECT_TIMEOUT_MS = 20000;

    public DuelManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void startDuel(DuelRequest request) {
        Player player1 = Bukkit.getPlayer(request.getSender());
        Player player2 = Bukkit.getPlayer(request.getTarget());
        if (player1 == null || player2 == null) return;

        savedStates.putIfAbsent(player1.getUniqueId(), new PlayerState(player1));
        savedStates.putIfAbsent(player2.getUniqueId(), new PlayerState(player2));

        Arena arena = null;

        String arenaName = request.getArenaName();
        if (arenaName != null) {
            arena = plugin.getArenaManager().getArena(arenaName);
        }

        if (arena == null) {
            // fallback (shouldn't happen often)
            arena = plugin.getArenaManager().getRandomAvailableArena();
            if (arena == null) {
                player1.sendMessage(plugin.getPrefix() + "§cNo available arenas!");
                player2.sendMessage(plugin.getPrefix() + "§cNo available arenas!");
                return;
            }
            arena.setInUse(true);
        } else {
            // arena was reserved in addDuelRequest, keep it inUse
            arena.setInUse(true);
        }

        // DuelSession erstellen
        DuelSession session = new DuelSession(
                player1.getUniqueId(),
                player2.getUniqueId(),
                request.getKitName(),
                arena.getName(),
                plugin.getConfigManager().getMainConfig().getInt("duel-time", 180),
                request.getBestOf()
        );

        // Session registrieren
        activeDuels.put(player1.getUniqueId(), session);
        activeDuels.put(player2.getUniqueId(), session);

        // Spieler vorbereiten
        preparePlayersForDuel(player1, player2, session, arena);

        // Countdown starten
        startDuelCountdown(player1, player2, session);
    }
    private void clearChat(Player player) {
        for (int i = 0; i < 100; i++) {
            player.sendMessage("");
        }
    }

    private void preparePlayersForDuel(Player p1, Player p2, DuelSession session, Arena arena) {
        // Inventar leeren
        forceRoundState(p1);
        forceRoundState(p2);

        p1.getInventory().clear();
        p2.getInventory().clear();
        p1.getInventory().setArmorContents(null);
        p2.getInventory().setArmorContents(null);

        // Kits geben
        plugin.getKitManager().giveKit(p1, session.getKitName());
        plugin.getKitManager().giveKit(p2, session.getKitName());

        // Teleportieren
        if (arena.getSpawn1() != null && arena.getSpawn2() != null) {
            p1.teleport(arena.getSpawn1());
            p2.teleport(arena.getSpawn2());
        }

        // Tab-Liste anpassen
        plugin.getPlayerManager().applyDuelVisibility(p1, p2);


        // Nachrichten senden
        clearChat(p1);
        clearChat(p2);
        String kitDisplay = plugin.getKitManager().getKitDisplayName(session.getKitName());

        p1.sendMessage(plugin.getPrefix() + "§aDuel started §7against §c" + p2.getName() + "!");
        p1.sendMessage(plugin.getPrefix() + "§7Kit: §r" + kitDisplay + " §7| Arena: §b" + session.getArenaName());
        p1.sendMessage(plugin.getPrefix() + "§dMatch: §fBest of " + session.getBestOf() + " §7(need " + session.requiredWins() + " wins)");

        p2.sendMessage(plugin.getPrefix() + "§aDuel started §7against §c" + p1.getName() + "!");
        p2.sendMessage(plugin.getPrefix() + "§7Kit: §r" + kitDisplay  + " §7| Arena: §b" + session.getArenaName());
        p2.sendMessage(plugin.getPrefix() + "§dMatch: §fBest of " + session.getBestOf() + " §7(need " + session.requiredWins() + " wins)");
    }

    private void startDuelCountdown(Player p1, Player p2, DuelSession session) {
        frozenPlayers.add(p1.getUniqueId());
        frozenPlayers.add(p2.getUniqueId());

        // Blindness
        p1.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false));
        p2.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false));

        new BukkitRunnable() {
            int time = 3;

            @Override
            public void run() {
                if (time > 0) {
                    p1.sendTitle("§c" + time, "§7Get ready", 0, 20, 0);
                    p2.sendTitle("§c" + time, "§7Get ready", 0, 20, 0);

                    float pitch = time == 3 ? 0.5f : (time == 2 ? 0.8f : 1.2f);
                    p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
                    p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);

                    time--;
                } else {
                    // FIGHT
                    p1.sendTitle("§aFIGHT!", "§7Best of " + session.getBestOf(), 0, 20, 10);
                    p2.sendTitle("§aFIGHT!", "§7Best of " + session.getBestOf(), 0, 20, 10);

                    p1.playSound(p1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                    p2.playSound(p2.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

                    p1.sendMessage("");
                    p2.sendMessage("");
                    p1.sendMessage(plugin.getPrefix() + "§a§lFIGHT! §7Round §f#1");
                    p2.sendMessage(plugin.getPrefix() + "§a§lFIGHT! §7Round §f#1");

                    frozenPlayers.remove(p1.getUniqueId());
                    frozenPlayers.remove(p2.getUniqueId());

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void handleDuelDeath(Player dead, Player killer, boolean disconnected) {
        UUID deadId = dead.getUniqueId();
        DuelSession session = activeDuels.get(deadId);
        if (session == null) return;

        // Anti-double trigger
        if (session.isRoundStarting()) return;
        session.setRoundStarting(true);

        UUID winnerId = session.getOpponent(deadId);
        if (winnerId == null) {
            session.setRoundStarting(false);
            return;
        }

        Player winner = Bukkit.getPlayer(winnerId);
        if (winner == null || !winner.isOnline()) {
            session.setRoundStarting(false);
            endDuel(deadId, killer, true);
            return;
        }

        // Score update
        if (session.getPlayer1().equals(winnerId)) {
            session.setWinsP1(session.getWinsP1() + 1);
        } else {
            session.setWinsP2(session.getWinsP2() + 1);
        }

        roundDead.add(deadId);

        // Names safe
        Player s1 = Bukkit.getPlayer(session.getPlayer1());
        Player s2 = Bukkit.getPlayer(session.getPlayer2());
        String n1 = (s1 != null) ? s1.getName() : "Player1";
        String n2 = (s2 != null) ? s2.getName() : "Player2";

        String scoreFormat = "§7" + n1 + " §8(§f" + session.getWinsP1() + " §7- §f" + session.getWinsP2() + "§8) §7" + n2;

        winner.sendMessage(plugin.getPrefix() + "§aYou won §7round §f#" + session.getRound() + " §8| " + scoreFormat);
        dead.sendMessage(plugin.getPrefix() + "§cYou lost §7round §f#" + session.getRound() + " §8| " + scoreFormat);

        // Match over?
        if (session.getWinsP1() >= session.requiredWins() || session.getWinsP2() >= session.requiredWins()) {
            session.setRoundStarting(false);
            endDuel(deadId, winner, disconnected);
            return;
        }

        // Next round
        session.setRound(session.getRound() + 1);
        startNextRound(session);
    }

    private void startNextRound(DuelSession session) {
        Player p1 = Bukkit.getPlayer(session.getPlayer1());
        Player p2 = Bukkit.getPlayer(session.getPlayer2());

        if (p1 == null || p2 == null) {
            UUID loser = (p1 == null) ? session.getPlayer1() : session.getPlayer2();
            endDuel(loser, null, true);
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(session.getArenaName());
        if (arena == null) {
            endDuelByTimeout(p1, p2);
            return;
        }


        // Arena resetten
        plugin.getArenaManager().resetArena(arena, () -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!isInDuel(p1.getUniqueId()) || !isInDuel(p2.getUniqueId())) return;

                // Teleportieren
                if (arena.getSpawn1() != null && arena.getSpawn2() != null) {
                    pendingRoundRespawn.put(session.getPlayer1(), arena.getSpawn1());
                    pendingRoundRespawn.put(session.getPlayer2(), arena.getSpawn2());
                }

                // Teleport und Vorbereitung
                p1.teleport(pendingRoundRespawn.getOrDefault(session.getPlayer1(), p1.getLocation()));
                p2.teleport(pendingRoundRespawn.getOrDefault(session.getPlayer2(), p2.getLocation()));

                forceRoundState(p1);
                forceRoundState(p2);

                plugin.getKitManager().giveKit(p1, session.getKitName());
                plugin.getKitManager().giveKit(p2, session.getKitName());

                runRoundCountdown(session, p1, p2);
            });
        });
    }

    private void runRoundCountdown(DuelSession session, Player p1, Player p2) {
        frozenPlayers.add(p1.getUniqueId());
        frozenPlayers.add(p2.getUniqueId());

        p1.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false));
        p2.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false));

        new BukkitRunnable() {
            int time = 3;

            @Override
            public void run() {
                if (time > 0) {
                    p1.sendTitle("§c" + time, "§7Get ready", 0, 20, 0);
                    p2.sendTitle("§c" + time, "§7Get ready", 0, 20, 0);

                    float pitch = time == 3 ? 0.5f : (time == 2 ? 0.8f : 1.2f);
                    p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
                    p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);

                    time--;
                } else {
                    p1.sendTitle("§aFIGHT!", "§7Best of " + session.getBestOf(), 0, 20, 10);
                    p2.sendTitle("§aFIGHT!", "§7Best of " + session.getBestOf(), 0, 20, 10);

                    p1.playSound(p1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                    p2.playSound(p2.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);

                    p1.sendMessage("");
                    p2.sendMessage("");
                    p1.sendMessage(plugin.getPrefix() + "§a§lFIGHT! §7Round §f#" + session.getRound());
                    p2.sendMessage(plugin.getPrefix() + "§a§lFIGHT! §7Round §f#" + session.getRound());

                    frozenPlayers.remove(p1.getUniqueId());
                    frozenPlayers.remove(p2.getUniqueId());
                    roundDead.remove(p1.getUniqueId());
                    roundDead.remove(p2.getUniqueId());
                    session.setRoundStarting(false);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void endDuel(UUID loserUUID, Player killer, boolean disconnected) {
        DuelSession session = activeDuels.get(loserUUID);
        if (session == null) return;

        UUID winnerUUID = session.getOpponent(loserUUID);

        Player loser = Bukkit.getPlayer(loserUUID);
        Player winner = winnerUUID != null ? Bukkit.getPlayer(winnerUUID) : null;

        // Stats aktualisieren
        if (winner != null && loser != null) {
            plugin.getPlayerManager().addStat(winner.getUniqueId(), "wins", 1);
            plugin.getPlayerManager().addStat(winner.getUniqueId(), "kills", 1);

            plugin.getPlayerManager().addStat(loser.getUniqueId(), "losses", 1);
            plugin.getPlayerManager().addStat(loser.getUniqueId(), "deaths", 1);
        }

        // Arena resetten wenn Match vorbei
        boolean matchOver = session.getWinsP1() >= session.requiredWins() || session.getWinsP2() >= session.requiredWins();

        if (matchOver) {
            Arena arena = plugin.getArenaManager().getArena(session.getArenaName());

            Runnable finish = () -> {
                if (arena != null) arena.setInUse(false);

                cleanupDuel(loserUUID, winnerUUID);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    teleportToSpawnSafe(loser);
                    teleportToSpawnSafe(winner);

                    restoreToLobby(loser);
                    restoreToLobby(winner);
                }, 3L);

            };

            Runnable finishSync = () -> Bukkit.getScheduler().runTask(plugin, finish);
            if (arena != null) plugin.getArenaManager().resetArena(arena, finishSync);
            else finishSync.run();


        } else {
            session.setRoundStarting(false);
        }
    }

    private void endDuelByTimeout(Player p1, Player p2) {
        UUID u1 = p1.getUniqueId();
        UUID u2 = p2.getUniqueId();

        DuelSession session = activeDuels.get(u1);
        if (session == null) return;

        int w1 = session.getWinsP1();
        int w2 = session.getWinsP2();

        Player winner = null;
        Player loser = null;

        // Gewinner bestimmen
        if (w1 > w2) {
            winner = p1;
            loser = p2;
        } else if (w2 > w1) {
            winner = p2;
            loser = p1;
        }

        // Nachrichten
        if (winner != null) {
            winner.sendMessage(plugin.getPrefix() + "§aYou won the duel §7by timeout!");
            loser.sendMessage(plugin.getPrefix() + "§cYou lost the duel §7by timeout.");

            // Stats
            plugin.getPlayerManager().addStat(winner.getUniqueId(), "wins", 1);
            plugin.getPlayerManager().addStat(winner.getUniqueId(), "kills", 1);

            plugin.getPlayerManager().addStat(loser.getUniqueId(), "losses", 1);
            plugin.getPlayerManager().addStat(loser.getUniqueId(), "deaths", 1);
        } else {
            // Draw
            p1.sendMessage(plugin.getPrefix() + "§eDuel ended in a draw §7(time ran out).");
            p2.sendMessage(plugin.getPrefix() + "§eDuel ended in a draw §7(time ran out).");
        }

        Arena arena = plugin.getArenaManager().getArena(session.getArenaName());

        Runnable finish = () -> {
            if (arena != null) arena.setInUse(false);


            teleportToSpawnSafe(p1);
            teleportToSpawnSafe(p2);

            restoreToLobby(p1);
            restoreToLobby(p2);

            cleanupDuel(u1, u2);
        };

        // IMPORTANT: immer sync ausführen
        Bukkit.getScheduler().runTask(plugin, finish);
    }


    private void restoreToLobby(Player p) {
        if (p == null || !p.isOnline()) return;

        plugin.getPlayerManager().forceLobbyState(p);
        plugin.getPlayerManager().setupPlayerInventory(p);
        plugin.getPlayerManager().refreshQueueSlotItem(p);
        plugin.getPlayerManager().applyLobbyFly(p);
        plugin.getScoreboardManager().updateScoreboard(p);
    }

    private void forceRoundState(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    public void cleanupDuel(UUID player1, UUID player2) {
        activeDuels.remove(player1);
        if (player2 != null) activeDuels.remove(player2);

        frozenPlayers.remove(player1);
        if (player2 != null) frozenPlayers.remove(player2);

        roundDead.remove(player1);
        if (player2 != null) roundDead.remove(player2);

        Player p1 = Bukkit.getPlayer(player1);
        Player p2 = player2 != null ? Bukkit.getPlayer(player2) : null;

        savedStates.remove(player1);
        if (player2 != null) savedStates.remove(player2);

        plugin.getPlayerManager().restoreAllVisibility();

    }

    public void cleanupAll() {
        for (DuelSession session : new HashSet<>(activeDuels.values())) {
            cleanupDuel(session.getPlayer1(), session.getPlayer2());
        }
        activeDuels.clear();
        duelRequests.clear();
        frozenPlayers.clear();
        pendingRoundRespawn.clear();
        roundDead.clear();
        autoSelect.clear();
        lastRequestMs.clear();
    }

    public void updateDuelTimers() {
        for (DuelSession session : new HashSet<>(activeDuels.values())) {
            if (session.getTimeLeft() > 0) {
                session.setTimeLeft(session.getTimeLeft() - 1);
            } else {
                Player p1 = Bukkit.getPlayer(session.getPlayer1());
                Player p2 = Bukkit.getPlayer(session.getPlayer2());
                if (p1 != null && p2 != null) {
                    endDuelByTimeout(p1, p2);
                }
            }
        }
    }

    public void addDuelRequest(UUID target, DuelRequest request) {
        UUID senderId = request.getSender();
        long now = System.currentTimeMillis();

        Player sender = Bukkit.getPlayer(senderId);
        Player receiver = Bukkit.getPlayer(target);

        // basic checks
        if (senderId.equals(target)) {
            if (sender != null) sender.sendMessage(plugin.getPrefix() + "§cYou can't duel yourself.");
            return;
        }
        if (isInDuel(senderId) || isInDuel(target)) {
            if (sender != null) sender.sendMessage(plugin.getPrefix() + "§cSomeone is already in a duel.");
            return;
        }

        // cooldown (per sender)
        Long last = lastRequestMs.get(senderId);
        if (last != null && (now - last) < REQUEST_COOLDOWN_MS) {
            long leftSec = (REQUEST_COOLDOWN_MS - (now - last) + 999) / 1000;
            if (sender != null) sender.sendMessage(plugin.getPrefix() + "§cWait §f" + leftSec + "s §cbefore sending another request.");
            return;
        }
        lastRequestMs.put(senderId, now);

        // Falls target schon eine Request hatte -> alte Arena freigeben
        DuelRequest old = duelRequests.remove(target);
        if (old != null) {
            Arena oldArena = plugin.getArenaManager().getArena(old.getArenaName());
            if (oldArena != null) oldArena.setInUse(false);
        }

        // Arena jetzt auswählen und RESERVIEREN
        Arena chosen = plugin.getArenaManager().getRandomAvailableArena();
        if (chosen == null) {
            if (sender != null) sender.sendMessage(plugin.getPrefix() + "§cNo available arenas!");
            if (receiver != null) receiver.sendMessage(plugin.getPrefix() + "§cNo available arenas!");
            return;
        }
        chosen.setInUse(true);

        // Neuen Request bauen, der genau diese Arena enthält
        DuelRequest stored = new DuelRequest(
                request.getSender(),
                request.getTarget(),
                request.getKitName(),
                chosen.getName(),
                request.getBestOf()
        );

        duelRequests.put(target, stored);

        String kitDisplay = plugin.getKitManager().getKitDisplayName(stored.getKitName());

        // sender feedback
        if (sender != null && sender.isOnline()) {
            sender.sendMessage("\n" + plugin.getPrefix() + "§aDuel request sent to §e" + (receiver != null ? receiver.getName() : "player") + "\n" +
                    plugin.getPrefix() + "§7Kit: §r" + kitDisplay + "§7 | Arena: §b" + chosen.getName() + "§7\n" +
                    plugin.getPrefix() + "§7Best of §f" + stored.getBestOf());
        }

        // receiver message
        if (receiver != null && receiver.isOnline()) {
            receiver.sendMessage("\n" + plugin.getPrefix() + "§e" + (sender != null ? sender.getName() : "Someone") +
                    " §7challenged you!\n" +
                    plugin.getPrefix() + "§7Kit: §r" + kitDisplay + "§7 | Arena: §b" + chosen.getName() + "§7\n" +
                    plugin.getPrefix() + "§7Best of §f" + stored.getBestOf());

            receiver.playSound(receiver.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            Component accept = Component.text("[ACCEPT]", NamedTextColor.GREEN)
                    .hoverEvent(HoverEvent.showText(Component.text("Click to accept", NamedTextColor.GREEN)))
                    .clickEvent(ClickEvent.runCommand("/duel accept"));

            Component deny = Component.text("[DENY]", NamedTextColor.RED)
                    .hoverEvent(HoverEvent.showText(Component.text("Click to deny", NamedTextColor.RED)))
                    .clickEvent(ClickEvent.runCommand("/duel deny"));

            Component msg = Component.text("» ", NamedTextColor.DARK_GRAY)
                    .append(accept)
                    .append(Component.text(" ", NamedTextColor.GRAY))
                    .append(deny)
                    .append(Component.text(" or type ", NamedTextColor.GRAY))
                    .append(Component.text("/duel accept", NamedTextColor.YELLOW));

            receiver.sendMessage(msg);
        }
    }


    public void acceptDuelRequest(Player target) {
        DuelRequest request = duelRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(plugin.getPrefix() + "§cNo pending duel request.");
            return;
        }
        if (request.isExpired(30)) { // z.B. 30 Sekunden
            Arena a = plugin.getArenaManager().getArena(request.getArenaName());
            if (a != null) a.setInUse(false);
            target.sendMessage(plugin.getPrefix() + "§cDuel request expired.");
            return;
        }
        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(plugin.getPrefix() + "§cRequester is offline.");
            return;
        }

        if (isInDuel(sender.getUniqueId()) || isInDuel(target.getUniqueId())) {
            target.sendMessage(plugin.getPrefix() + "§cSomeone is already in a duel.");
            return;
        }

        // start duel
        startDuel(request);
    }

    public void denyDuelRequest(Player target) {
        DuelRequest request = duelRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage(plugin.getPrefix() + "§cNo pending duel request.");
            return;
        }

        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(plugin.getPrefix() + "§cYour duel request was denied.");
        }
        target.sendMessage(plugin.getPrefix() + "§7Request denied.");
        Arena a = plugin.getArenaManager().getArena(request.getArenaName());
        if (a != null) a.setInUse(false);
    }

    public DuelRequest getDuelRequest(UUID target) {
        return duelRequests.get(target);
    }

    public void removeDuelRequest(UUID target) {
        duelRequests.remove(target);
    }

    // Füge diese Methoden zur DuelManager Klasse hinzu:

    public int getActiveDuelCount() {
        // Zählt einzigartige Duels (jedes Duel hat 2 Spieler)
        Set<DuelSession> uniqueSessions = new HashSet<>(activeDuels.values());
        return uniqueSessions.size();
    }

    public Collection<DuelSession> getAllSessions() {
        // Gibt alle einzigartigen Sessions zurück
        return new HashSet<>(activeDuels.values());
    }

    public void handlePlayerDisconnect(UUID playerUUID) {
        DuelSession session = activeDuels.get(playerUUID);
        if (session == null) return;

        UUID opponentUUID = session.getOpponent(playerUUID);
        Player opponent = Bukkit.getPlayer(opponentUUID);

        // Opponent benachrichtigen
        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(plugin.getPrefix() + "§cYour opponent disconnected! You win!");

            // Stats für Gegner
            plugin.getPlayerManager().addStat(opponentUUID, "wins", 1);
            plugin.getPlayerManager().addStat(opponentUUID, "kills", 1);

            // Stats für disconnected Spieler
            plugin.getPlayerManager().addStat(playerUUID, "losses", 1);
            plugin.getPlayerManager().addStat(playerUUID, "deaths", 1);
        }

        // Arena zurücksetzen
        Arena arena = plugin.getArenaManager().getArena(session.getArenaName());
        if (arena != null) {
            arena.setInUse(false);
            plugin.getArenaManager().resetArena(arena, null);
        }

        // Duel aufräumen
        cleanupDuel(playerUUID, opponentUUID);

        // Gegner zum Spawn teleportieren
        if (opponent != null && opponent.isOnline()) {
            plugin.getPlayerManager().teleportToSpawn(opponent);
        }
    }

    public void handleForfeit(Player player) {
        UUID playerUUID = player.getUniqueId();
        DuelSession session = activeDuels.get(playerUUID);

        if (session == null) {
            player.sendMessage(plugin.getPrefix() + "§cYou are not in a duel!");
            return;
        }

        UUID opponentUUID = session.getOpponent(playerUUID);
        Player opponent = Bukkit.getPlayer(opponentUUID);

        // Nachrichten senden
        player.sendMessage(plugin.getPrefix() + "§cYou forfeited the duel! §7This counts as a loss.");

        if (opponent != null && opponent.isOnline()) {
            opponent.sendMessage(plugin.getPrefix() + "§a" + player.getName() + " §7forfeited the duel! §aYou win!");
        }

        // Stats aktualisieren
        plugin.getPlayerManager().addStat(playerUUID, "losses", 1);
        plugin.getPlayerManager().addStat(playerUUID, "deaths", 1);

        if (opponent != null) {
            plugin.getPlayerManager().addStat(opponentUUID, "wins", 1);
            plugin.getPlayerManager().addStat(opponentUUID, "kills", 1);
        }

        // Arena zurücksetzen
        Arena arena = plugin.getArenaManager().getArena(session.getArenaName());
        if (arena != null) {
            arena.setInUse(false);
            plugin.getArenaManager().resetArena(arena, null);
        }

        // Duel aufräumen
        cleanupDuel(playerUUID, opponentUUID);

        // Beide Spieler zum Spawn teleportieren
        plugin.getPlayerManager().teleportToSpawn(player);
        if (opponent != null && opponent.isOnline()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getPlayerManager().teleportToSpawn(opponent);
            }, 2L);
        }
    }

    public boolean isInDuel(UUID uuid) {
        return activeDuels.containsKey(uuid);
    }

    public DuelSession getDuelSession(UUID uuid) {
        return activeDuels.get(uuid);
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public Location getPendingRespawn(UUID uuid) {
        return pendingRoundRespawn.get(uuid);
    }

    public void removePendingRespawn(UUID uuid) {
        pendingRoundRespawn.remove(uuid);
    }

    public boolean isRoundDead(UUID uuid) {
        return roundDead.contains(uuid);
    }

    public void handleAutoSelect(Player picker, Player target, String kitName, int bestOf) {
        // Auto-Select-Logik
    }

    // Innere Klassen für AutoSelect
    private static class PairKey {
        final UUID a, b;

        PairKey(UUID x, UUID y) {
            if (x.compareTo(y) <= 0) {
                this.a = x;
                this.b = y;
            } else {
                this.a = y;
                this.b = x;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PairKey)) return false;
            PairKey k = (PairKey) o;
            return a.equals(k.a) && b.equals(k.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }
    }

    private static class AutoSelect {
        final UUID firstPicker;
        final String kitName;
        final int bestOf;
        final long timestamp;

        AutoSelect(UUID firstPicker, String kitName, int bestOf) {
            this.firstPicker = firstPicker;
            this.kitName = kitName;
            this.bestOf = bestOf;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > AUTOSELECT_TIMEOUT_MS;
        }
    }
}