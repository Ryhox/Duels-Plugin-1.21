package dev.duels.listeners;

import dev.duels.DuelsPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class HotbarLockListener implements Listener {

    private final DuelsPlugin plugin;

    public HotbarLockListener(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean shouldLock(Player player) {
        if (player == null) return false;

        // In duel → allow everything
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) return false;

        // Creative → allow everything
        if (player.getGameMode() == GameMode.CREATIVE) return false;

        // Lobby → lock
        return plugin.getPlayerManager().isInLobby(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (shouldLock(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (shouldLock(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!shouldLock(player)) return;

        String title = event.getView().getTitle();

        // allow clicks in your custom GUIs (GUIListener will handle cancelling)
        if (title.equals(dev.duels.guis.GUIManager.QUEUE_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.BESTOF_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.EDIT_LAYOUTS_GUI_TITLE)
                || title.startsWith(dev.duels.guis.GUIManager.EDIT_LAYOUT_GUI_TITLE_PREFIX)
                || title.equals(dev.duels.guis.GUIManager.DUEL_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.KITS_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.SETTINGS_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.STATS_GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!shouldLock(player)) return;

        String title = event.getView().getTitle();

        if (title.equals(dev.duels.guis.GUIManager.QUEUE_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.BESTOF_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.EDIT_LAYOUTS_GUI_TITLE)
                || title.startsWith(dev.duels.guis.GUIManager.EDIT_LAYOUT_GUI_TITLE_PREFIX)
                || title.equals(dev.duels.guis.GUIManager.DUEL_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.KITS_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.SETTINGS_GUI_TITLE)
                || title.equals(dev.duels.guis.GUIManager.STATS_GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreativeInventory(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!shouldLock(player)) return;

        event.setCancelled(true);
        player.updateInventory();
    }
}
