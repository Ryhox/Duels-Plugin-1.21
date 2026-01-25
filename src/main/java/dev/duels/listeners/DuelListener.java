package dev.duels.listeners;

import dev.duels.DuelsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class DuelListener implements Listener {

    private final DuelsPlugin plugin;

    public DuelListener(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        // In Duel?
        if (plugin.getDuelManager().isInDuel(dead.getUniqueId())) {
            event.setDeathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepInventory(true);
            event.setKeepLevel(true);

            // Duel Death behandeln
            Bukkit.getScheduler().runTask(plugin, () ->
                    plugin.getDuelManager().handleDuelDeath(dead, killer, false));
            return;
        }

        // Normaler Death - Stats updaten
        plugin.getPlayerManager().addStat(dead.getUniqueId(), "deaths", 1);
        if (killer != null) {
            plugin.getPlayerManager().addStat(killer.getUniqueId(), "kills", 1);
        }

        // Scoreboards updaten
        plugin.getScoreboardManager().updateScoreboard(dead);
        if (killer != null) {
            plugin.getScoreboardManager().updateScoreboard(killer);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // In Duel - Schaden erlauben
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            return;
        }

        // In Creative - Schaden erlauben
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Im Lobby - Schaden blockieren
        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // In Duel - Schaden erlauben
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            return;
        }

        // In Creative - Schaden erlauben
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Diamond Sword Hit für Duel Request
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (damager.getInventory().getItemInMainHand().getType() == Material.DIAMOND_SWORD) {
                event.setCancelled(true);

                // Duel Request senden
                if (!plugin.getDuelManager().isInDuel(damager.getUniqueId()) &&
                        !plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                    plugin.getGuiManager().openDuelGUI(damager, player);
                }
                return;
            }
        }

        // Im Lobby - Schaden blockieren
        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // In Duel - Item Drop verhindern
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // In Survival - Item Drop verhindern
        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // In Duel - Item Drop verhindern
            if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            // In Survival - Item Drop verhindern
            if (player.getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCraftItem(org.bukkit.event.inventory.CraftItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(plugin.getPrefix() + "§cBeds are disabled! Spawn is fixed.");
    }
}