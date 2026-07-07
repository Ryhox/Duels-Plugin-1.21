package dev.duels.listeners;

import dev.duels.DuelsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.projectiles.ProjectileSource;

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

        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            if (event instanceof EntityDamageByEntityEvent) {
                return;
            }
            if (handleDuelDamage(event, player, null)) {
                return;
            }
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

        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            Player attacker = getAttackingPlayer(event.getDamager());
            if (attacker == null || !isDuelOpponent(player, attacker)) {
                event.setCancelled(true);
                return;
            }

            if (handleDuelDamage(event, player, attacker)) {
                return;
            }
            return;
        }

        // In Creative - Schaden erlauben
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Diamond Sword Hit für Duel Request
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            org.bukkit.inventory.ItemStack held = damager.getInventory().getItemInMainHand();
            if (held.getType() == Material.DIAMOND_SWORD
                    && held.hasItemMeta()
                    && held.getItemMeta().getDisplayName().contains("ᴄʜᴀʟʟᴇɴɢᴇ")) {
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

    private boolean handleDuelDamage(EntityDamageEvent event, Player player, Player attacker) {
        if (event.isCancelled()) return true;

        if (plugin.getDuelManager().isFrozen(player.getUniqueId())
                || plugin.getDuelManager().isRoundDead(player.getUniqueId())) {
            event.setCancelled(true);
            return true;
        }

        if (event.getFinalDamage() < player.getHealth()) {
            return false;
        }

        event.setCancelled(true);
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setNoDamageTicks(0);
        player.setInvulnerable(false);
        player.setHealth(player.getMaxHealth());

        plugin.getDuelManager().handleDuelDeath(player, attacker, false);
        return true;
    }

    private Player getAttackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }

        return null;
    }

    private boolean isDuelOpponent(Player player, Player attacker) {
        if (player.equals(attacker)) return true;
        dev.duels.objects.DuelSession session = plugin.getDuelManager().getDuelSession(player.getUniqueId());
        return session != null && session.contains(attacker.getUniqueId());
    }
}
