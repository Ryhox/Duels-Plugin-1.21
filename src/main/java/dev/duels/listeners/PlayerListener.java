package dev.duels.listeners;

import dev.duels.DuelsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final DuelsPlugin plugin;

    public PlayerListener(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getPlayerManager().loadPlayerData(uuid);
        plugin.getPlayerManager().getPlayerData(uuid).setName(player.getName());

        forceLobbyState(player);

        Location spawn = plugin.getArenaManager().getSpawnLocation();
        if (spawn != null) player.teleport(spawn);

        plugin.getPlayerManager().setupPlayerInventory(player);

        plugin.getPlayerManager().updatePlayerVisibility(player);
        plugin.getPlayerManager().handleJoinVisibility(player);
        plugin.getPlayerManager().enforceDuelPrivacyForJoin(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getPlayerManager().applyLobbyFly(player);
        }, 2L);

        plugin.getScoreboardManager().updateScoreboard(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.getQueueManager().leaveAllQueues(uuid);

        if (plugin.getDuelManager().isInDuel(uuid)) {
            plugin.getDuelManager().handlePlayerDisconnect(uuid);
        }

        plugin.getScoreboardManager().removeScoreboard(uuid);
        plugin.getPlayerManager().savePlayerData(uuid);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Location pendingRespawn = plugin.getDuelManager().getPendingRespawn(uuid);
        if (pendingRespawn != null && plugin.getDuelManager().isInDuel(uuid)) {
            event.setRespawnLocation(pendingRespawn);
            plugin.getDuelManager().removePendingRespawn(uuid);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getDuelManager().isInDuel(uuid)) {
                    forceRoundState(player);
                    plugin.getKitManager().giveKit(player,
                            plugin.getDuelManager().getDuelSession(uuid).getKitName());
                }
            }, 1L);
            return;
        }

        Location spawn = plugin.getArenaManager().getSpawnLocation();
        if (spawn != null) event.setRespawnLocation(spawn);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.getDuelManager().isInDuel(uuid)) {
                forceLobbyState(player);
                plugin.getPlayerManager().setupPlayerInventory(player);
                spawnParticlesCircle(player);
                plugin.getPlayerManager().applyLobbyFly(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();

        if (event.getNewGameMode() == GameMode.SURVIVAL) {
            if (!plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                plugin.getPlayerManager().setupPlayerInventory(player);
            }
        } else if (event.getNewGameMode() == GameMode.CREATIVE) {
            player.getInventory().clear();
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                event.setCancelled(true);
                player.setFoodLevel(20);
                player.setSaturation(20f);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDuelManager().isFrozen(player.getUniqueId())) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // ✅ Only react on RIGHT CLICK (prevents “hit air with sword” triggering this)
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> { }
            default -> {
                return;
            }
        }

        if (event.getItem() == null || !event.getItem().hasItemMeta()) return;

        String displayName = event.getItem().getItemMeta().getDisplayName();
        if (displayName == null) return;

        // Queue Items
        if (displayName.contains("ʟᴇᴀᴠᴇ ǫᴜᴇᴜᴇ")) {
            event.setCancelled(true);

            plugin.getQueueManager().leaveQueue(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 0.8f);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getPlayerManager().refreshQueueSlotItem(player);
            }, 1L);

            return;
        }


        if (displayName.contains("ᴊᴏɪɴ ʟᴀѕᴛ ǫᴜᴇᴜᴇ")) {
            event.setCancelled(true);

            String lastKit = plugin.getQueueManager().getLastQueueKit(player.getUniqueId());
            if (lastKit != null) {
                plugin.getQueueManager().joinQueue(player, lastKit);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getPlayerManager().refreshQueueSlotItem(player);
                }, 1L);

            } else {
                player.sendMessage(plugin.getPrefix() + "§cNo last queue kit saved yet!");
            }
            return;
        }

        // Challenge Sword
        if (displayName.contains("ᴄʜᴀʟʟᴇɴɢᴇ")) {
            event.setCancelled(true);
            if (!plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                plugin.getGuiManager().openQueueGUI(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        // Stats Book
        if (displayName.contains("ѕᴛᴀᴛѕ")) {
            event.setCancelled(true);
            plugin.getGuiManager().openStatsGUI(player);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return;
        }

        // Settings
        if (displayName.contains("ѕᴇᴛᴛɪɴɢѕ")) {
            event.setCancelled(true);
            plugin.getGuiManager().openSettingsGUI(player);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return;
        }

// Visibility Toggle
        if (displayName.contains("ᴘʟᴀʏᴇʀ ᴠɪѕɪʙɪʟɪᴛʏ")) {
            event.setCancelled(true);

            plugin.getPlayerManager().toggleVisibility(player.getUniqueId());
            plugin.getPlayerManager().updatePlayerVisibility(player);
            plugin.getPlayerManager().applyVisibility(player);

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            return;
        }


    }

    private void forceLobbyState(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setRemainingAir(player.getMaximumAir());
        player.setFreezeTicks(0);
        player.setAbsorptionAmount(0.0);

        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType()));

        player.setGameMode(GameMode.SURVIVAL);
    }

    private void forceRoundState(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setRemainingAir(player.getMaximumAir());
        player.setFreezeTicks(0);

        player.getActivePotionEffects().forEach(effect ->
                player.removePotionEffect(effect.getType()));

        player.setAbsorptionAmount(0.0);
    }


    private void spawnParticlesCircle(Player player) {
        Location loc = player.getLocation().clone().add(0, 1, 0);
        new BukkitRunnable() {
            double t = 0;
            final double radius = 1.2;
            final int points = 20;
            final int duration = 40;

            @Override
            public void run() {
                if (t > duration) {
                    cancel();
                    return;
                }
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points + t * 0.1;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    loc.getWorld().spawnParticle(Particle.END_ROD,
                            loc.clone().add(x, 0, z), 0, 0, 0, 0, 0);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
