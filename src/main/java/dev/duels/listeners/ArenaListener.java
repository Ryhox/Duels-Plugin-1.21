package dev.duels.listeners;

import dev.duels.DuelsPlugin;
import dev.duels.objects.Arena;
import dev.duels.objects.BlockVector;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ArenaListener implements Listener {

    private final DuelsPlugin plugin;

    public ArenaListener(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Creative immer erlauben
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // In Duel?
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            Location blockLoc = event.getBlock().getLocation();
            Arena arena = plugin.getArenaManager().getArenaAt(blockLoc);

            if (arena != null && arena.isInArena(blockLoc)) {
                BlockVector vector = new BlockVector(
                        blockLoc.getBlockX(),
                        blockLoc.getBlockY(),
                        blockLoc.getBlockZ()
                );

                // Nur player-placed blocks dürfen zerstört werden
                if (arena.isPlayerPlacedBlock(vector)) {
                    arena.removePlayerPlacedBlock(vector);
                    return; // Erlaubt
                } else {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getPrefix() + "§cYou cannot destroy arena blocks!");
                    return;
                }
            } else {
                event.setCancelled(true);
                return;
            }
        }

        // Nicht im Duel + nicht Creative -> blockieren
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Creative immer erlauben
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // In Duel?
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            Location blockLoc = event.getBlock().getLocation();
            Arena arena = plugin.getArenaManager().getArenaAt(blockLoc);

            if (arena != null && arena.isInArena(blockLoc)) {
                // Block als player-placed markieren
                BlockVector vector = new BlockVector(
                        event.getBlock().getX(),
                        event.getBlock().getY(),
                        event.getBlock().getZ()
                );
                arena.addPlayerPlacedBlock(vector);
                return; // Erlaubt
            } else {
                event.setCancelled(true);
                return;
            }
        }

        // Nicht im Duel + nicht Creative -> blockieren
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Location loc = event.getBlock().getLocation();
        Arena arena = plugin.getArenaManager().getArenaAt(loc);

        if (arena != null && arena.isInUse()) {
            // Nur player-placed blocks explodieren lassen
            event.blockList().removeIf(block -> {
                BlockVector v = new BlockVector(block.getX(), block.getY(), block.getZ());
                return !arena.isPlayerPlacedBlock(v);
            });

            // Player-placed blocks aus Tracking entfernen
            for (Block block : event.blockList()) {
                BlockVector v = new BlockVector(block.getX(), block.getY(), block.getZ());
                arena.removePlayerPlacedBlock(v);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = event.getLocation();
        Arena arena = plugin.getArenaManager().getArenaAt(loc);

        if (arena != null && arena.isInUse()) {
            // Nur player-placed blocks explodieren lassen
            event.blockList().removeIf(block -> {
                BlockVector v = new BlockVector(block.getX(), block.getY(), block.getZ());
                return !arena.isPlayerPlacedBlock(v);
            });

            // Player-placed blocks aus Tracking entfernen
            for (Block block : event.blockList()) {
                BlockVector v = new BlockVector(block.getX(), block.getY(), block.getZ());
                arena.removePlayerPlacedBlock(v);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Location loc = event.getBlock().getLocation();
        Arena arena = plugin.getArenaManager().getArenaAt(loc);

        if (arena != null && arena.isInUse()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Location loc = event.getLocation();
        Arena arena = plugin.getArenaManager().getArenaAt(loc);

        if (arena != null && arena.isInUse()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Location from = event.getBlock().getLocation();
        Location to = event.getToBlock().getLocation();

        Arena aFrom = plugin.getArenaManager().getArenaAt(from);
        Arena aTo = plugin.getArenaManager().getArenaAt(to);

        boolean fromInUse = aFrom != null && aFrom.isInUse() && aFrom.isInArena(from);
        boolean toInUse = aTo != null && aTo.isInUse() && aTo.isInArena(to);

        if (fromInUse && toInUse) return;

        if (fromInUse || toInUse) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Location loc = event.getBlock().getLocation();
        Arena arena = plugin.getArenaManager().getArenaAt(loc);

        if (arena != null && arena.isInUse()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Location loc = event.getBlock().getLocation();
        Arena arena = plugin.getArenaManager().getArenaAt(loc);

        if (arena != null && arena.isInUse()) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        Location loc = event.getBlock().getLocation();
        Arena arena = plugin.getArenaManager().getArenaAt(loc);

        if (arena != null && arena.isInUse()) {
            event.setCancelled(true);
        }
    }
}