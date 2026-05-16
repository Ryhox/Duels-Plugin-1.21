package dev.duels.listeners;

import dev.duels.DuelsPlugin;
import dev.duels.guis.GUIManager;
import dev.duels.managers.DuelManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.inventory.InventoryAction;
import dev.duels.objects.DuelRequest;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.UUID;
import java.util.Set;
import java.util.List;

public class GUIListener implements Listener {

    private final DuelsPlugin plugin;

    private final NamespacedKey queueKitKey;
    private final NamespacedKey previewKitKey;
    private final NamespacedKey duelKitKey;
    private final NamespacedKey editKitKey;
    private final NamespacedKey bestOfValueKey;
    private final NamespacedKey duelTargetKey;
    private final Set<UUID> awaitingStatsSearch = new java.util.HashSet<>();


    public GUIListener(DuelsPlugin plugin) {
        this.plugin = plugin;

        this.queueKitKey = new NamespacedKey(plugin, "queue_kit");
        this.previewKitKey = new NamespacedKey(plugin, "preview_kit");
        this.duelKitKey = new NamespacedKey(plugin, "duel_kit");
        this.editKitKey = new NamespacedKey(plugin, "edit_kit");
        this.bestOfValueKey = new NamespacedKey(plugin, "bestof_value");
        this.duelTargetKey = new NamespacedKey(plugin, "duel_target");
    }
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!awaitingStatsSearch.contains(uuid)) return;

        event.setCancelled(true);

        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (msg.equalsIgnoreCase("cancel")) {
            awaitingStatsSearch.remove(uuid);
            player.sendMessage(plugin.getPrefix() + "§7Search cancelled.");
            return;
        }

        UUID targetUuid = plugin.getPlayerManager().getUUIDFromName(msg);
        if (targetUuid == null) {
            player.sendMessage(plugin.getPrefix() + "§cPlayer not found: §7" + msg);
            player.sendMessage(plugin.getPrefix() + "§7Try again or type §ccancel§7.");
            return;
        }

        awaitingStatsSearch.remove(uuid);

        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getGuiManager().openCompareGUI(player, targetUuid);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        awaitingStatsSearch.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        Inventory top = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();

        // In Duel - Inventarbewegungen erlauben
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            return;
        }

        // Queue GUI
        if (title.equals(GUIManager.QUEUE_GUI_TITLE)) {
            handleQueueGUIClick(event, player, top, clickedInv);
            return;
        }

        // BestOf GUI
        if (title.equals(GUIManager.BESTOF_GUI_TITLE)) {
            handleBestOfGUIClick(event, player, top, clickedInv);
            return;
        }

        // Edit Layout GUI
        if (title.startsWith(GUIManager.EDIT_LAYOUT_GUI_TITLE_PREFIX)) {
            handleEditLayoutGUIClick(event, player, top, clickedInv);
            return;
        }

        // Edit Layouts Selection GUI
        if (title.equals(GUIManager.EDIT_LAYOUTS_GUI_TITLE)) {
            handleEditLayoutsGUIClick(event, player, top, clickedInv);
            return;
        }

        // Duel Kit Selection GUI
        if (title.equals(GUIManager.DUEL_GUI_TITLE)) {
            handleDuelGUIClick(event, player, top, clickedInv);
            return;
        }

        // Kits GUI
        if (title.equals(GUIManager.KITS_GUI_TITLE)) {
            handleKitsGUIClick(event, player, top, clickedInv);
            return;
        }

        // Settings GUI
        if (title.equals(GUIManager.SETTINGS_GUI_TITLE)) {
            handleSettingsGUIClick(event, player, top, clickedInv);
            return;
        }

        // Stats GUI
// Stats GUI
        if (title.equals(GUIManager.STATS_GUI_TITLE)) {
            event.setCancelled(true);

            int raw = event.getRawSlot();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = clicked.getItemMeta().getDisplayName();
            if (name == null) name = "";

            // Close
            if (name.contains("§cClose")) {
                player.closeInventory();
                plugin.getGuiManager().closeGUI(player.getUniqueId());
                return;
            }

            // Top 5 toggle (slot 11)
            if (raw == 11) {
                // re-open stats GUI (it reads next sort)
                plugin.getGuiManager().cycleStatsSortAndReopen(player);
                return;
            }

            // Search (slot 15)
            if (raw == 15 && name.contains("Search Players")) {
                player.closeInventory();
                plugin.getGuiManager().closeGUI(player.getUniqueId());

                awaitingStatsSearch.add(player.getUniqueId());
                player.sendMessage(plugin.getPrefix() + "§7Type a player name in chat. §7or Type §ccancel");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                return;
            }

            return;
        }
// Compare Stats GUI
        if (title.equals(GUIManager.COMPARE_GUI_TITLE)) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = clicked.getItemMeta().getDisplayName();
            if (name == null) name = "";

            if (name.contains("§cClose")) {
                player.closeInventory();
                plugin.getGuiManager().closeGUI(player.getUniqueId());
            }
            return;
        }

    }

    private void handleQueueGUIClick(InventoryClickEvent event, Player player, Inventory top, Inventory clickedInv) {
        event.setCancelled(true);
        if (clickedInv != top) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String display = meta.getDisplayName() == null ? "" : meta.getDisplayName();

        // Close Button / Info
        if (display.equals("§cClose") || display.equals("§6Queue: Select a Kit")) {
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player.getUniqueId());
            return;
        }

        // Kit Item via PDC
        String kitId = meta.getPersistentDataContainer().get(queueKitKey, PersistentDataType.STRING);
        if (kitId == null || kitId.isEmpty()) return;

        if (plugin.getQueueManager().isInQueue(player.getUniqueId(), kitId)) {
            plugin.getQueueManager().leaveQueue(player);
        } else {
            plugin.getQueueManager().joinQueue(player, kitId);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // refresh GUI
            plugin.getGuiManager().populateQueueGUI(player, top);
            player.updateInventory();

            // refresh hotbar item (slot 4 head/barrier)
            plugin.getPlayerManager().refreshQueueSlotItem(player);
        }, 1L);
    }


    private void handleBestOfGUIClick(InventoryClickEvent event, Player player, Inventory top, Inventory clickedInv) {
        event.setCancelled(true);
        if (clickedInv != top) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName() == null ? "" : meta.getDisplayName();

        // Close / Back
        if (name.equals("§cClose") || name.equals("§cBack")) {
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player.getUniqueId());
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Integer bestOf = pdc.get(bestOfValueKey, PersistentDataType.INTEGER);
        String kitId = pdc.get(duelKitKey, PersistentDataType.STRING);
        String targetStr = pdc.get(duelTargetKey, PersistentDataType.STRING);

        if (bestOf == null || kitId == null || kitId.isEmpty() || targetStr == null || targetStr.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + "§cMissing duel data (bestof/kit/target).");
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player.getUniqueId());
            return;
        }

        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(targetStr);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(plugin.getPrefix() + "§cInvalid target data.");
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player.getUniqueId());
            return;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getPrefix() + "§cTarget player is offline.");
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player.getUniqueId());
            return;
        }

        DuelRequest request = new DuelRequest(
                player.getUniqueId(),
                target.getUniqueId(),
                kitId,
                null,
                bestOf
        );

        plugin.getDuelManager().addDuelRequest(target.getUniqueId(), request);

        player.closeInventory();
        plugin.getGuiManager().closeGUI(player.getUniqueId());

        // Optional: feedback
        player.sendMessage(plugin.getPrefix() + "§7Sent duel request to §c" + target.getName()
                + " §7| Kit: §e" + kitId + " §7| Best of §f" + bestOf);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }


    private void handleEditLayoutGUIClick(InventoryClickEvent event, Player player, Inventory top, Inventory clickedInv) {
        final int raw = event.getRawSlot();

        // Immer: Shift/Quick-move blocken (sonst Copy/Dupe-Effekte)
        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            return;
        }

        // Zahlentasten / Offhand / Doubleclick / Middle blocken (Dupe/Weird)
        switch (event.getClick()) {
            case NUMBER_KEY:
            case SWAP_OFFHAND:
            case DOUBLE_CLICK:
            case MIDDLE:
                event.setCancelled(true);
                return;
            default:
                break;
        }

        // Klick im Player-Inventar (bottom): alles ignorieren, aber Hotbar 0-8 sperren
        if (clickedInv != null && clickedInv.equals(event.getView().getBottomInventory())) {
            int slot = event.getSlot(); // 0-35 bottom
            if (slot >= 0 && slot <= 8) {
                event.setCancelled(true);
            }
            return;
        }

        // Nur Top-Inventar behandeln
        if (clickedInv != top) return;

        // Buttons (unten rechts): 51/52/53
        if (raw == 51 || raw == 52 || raw == 53) {
            event.setCancelled(true);

            ItemStack button = event.getCurrentItem();
            if (button == null || !button.hasItemMeta()) return;

            String kitId = getEditLayoutKitId(top); // liest Slot 45
            if (kitId == null) {
                player.sendMessage(plugin.getPrefix() + "§cCould not detect kit id for this layout.");
                player.closeInventory();
                plugin.getGuiManager().closeGUI(player.getUniqueId());
                return;
            }

            String name = button.getItemMeta().getDisplayName();
            if (name == null) name = "";

            if (name.contains("§aSave Layout")) {
                ItemStack[] layout = new ItemStack[36];
                for (int i = 0; i < 36; i++) {
                    ItemStack it = top.getItem(i);
                    layout[i] = (it == null || it.getType() == Material.AIR) ? null : it.clone();
                }

                plugin.getKitManager().saveCustomLayout(player.getUniqueId(), kitId, layout);
                String kitDisplay = plugin.getKitManager().getKitDisplayName(kitId);

                player.sendMessage(plugin.getPrefix() + "§7Inventory layout for kit §r" + kitDisplay + " §asaved!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                player.closeInventory();
                plugin.getGuiManager().closeGUI(player.getUniqueId());
                return;
            }

            if (name.contains("§cReset to Default")) {
                String kitDisplay = plugin.getKitManager().getKitDisplayName(kitId);

                plugin.getKitManager().deleteCustomLayout(player.getUniqueId(), kitId);
                player.sendMessage(plugin.getPrefix() + "§7Inventory layout for kit §r" + kitDisplay + " §a reset to default!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getGuiManager().openEditLayoutGUI(player, kitId), 2L);
                return;
            }

            if (name.contains("§cClose")) {
                player.closeInventory();
                plugin.getGuiManager().closeGUI(player.getUniqueId());
            }
            return;
        }

        // Sperre Armor/Offhand-Anzeige 36-40 (und allgemein 36-44)
        if (raw >= 36 && raw <= 44) {
            event.setCancelled(true);
            return;
        }

        // Info-Paper Slot 45 sperren (damit kitId drin bleibt)
        if (raw == 45) {
            event.setCancelled(true);
            return;
        }

        // Rest unten (46-50, 54er GUI-filler usw.) sperren
        if (raw >= 46) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(false);
    }



    private void handleEditLayoutsGUIClick(InventoryClickEvent event, Player player, Inventory top, Inventory clickedInv) {
        event.setCancelled(true);
        if (clickedInv != top) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName() == null ? "" : meta.getDisplayName();

        if (name.equals("§cClose")) {
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player.getUniqueId());
            return;
        }

        if (name.equals("§6Select a Kit to Edit")) return;

        String kitId = meta.getPersistentDataContainer().get(editKitKey, PersistentDataType.STRING);
        if (kitId == null || kitId.isEmpty()) return;

        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getGuiManager().openEditLayoutGUI(player, kitId), 2L);
    }

    private void handleDuelGUIClick(InventoryClickEvent event, Player player, Inventory top, Inventory clickedInv) {
        event.setCancelled(true);
        if (clickedInv != top) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName() == null ? "" : meta.getDisplayName();

        if (name.contains("§cClose") || name.equals("§6Select a Kit")) {
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player.getUniqueId());
            return;
        }

        String kitId = meta.getPersistentDataContainer().get(duelKitKey, PersistentDataType.STRING);
        if (kitId == null || kitId.isEmpty()) return;

        String targetStr = meta.getPersistentDataContainer().get(duelTargetKey, PersistentDataType.STRING);
        if (targetStr == null || targetStr.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + "§cCould not find target player!");
            return;
        }

        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(targetStr);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(plugin.getPrefix() + "§cInvalid target data.");
            return;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getPrefix() + "§cTarget player not found or offline!");
            return;
        }

        if (event.getClick().isRightClick()) {
            plugin.getGuiManager().openBestOfGUI(player, target, kitId);
            return;
        }

        int defaultBestOf = plugin.getConfigManager().getMainConfig().getInt("default-bestof", 1);

        DuelRequest request = new DuelRequest(
                player.getUniqueId(),
                target.getUniqueId(),
                kitId,
                null,
                defaultBestOf
        );

        plugin.getDuelManager().addDuelRequest(target.getUniqueId(), request);

        player.closeInventory();
        plugin.getGuiManager().closeGUI(player.getUniqueId());
    }

    private void handleKitsGUIClick(InventoryClickEvent event, Player player, Inventory top, Inventory clickedInv) {
        event.setCancelled(true);
        if (clickedInv != top) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName() == null ? "" : meta.getDisplayName();

        if (name.contains("§cClose") || name.contains("§6Select a Kit")) {
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player.getUniqueId());
            return;
        }

        if (name.contains("§cNo Kits Available")) return;

        String kitId = meta.getPersistentDataContainer().get(previewKitKey, PersistentDataType.STRING);
        if (kitId == null || kitId.isEmpty()) return;

        player.closeInventory();
        plugin.getKitManager().giveKitPreview(player, kitId);
        plugin.getGuiManager().closeGUI(player.getUniqueId());
    }

    private void handleSettingsGUIClick(InventoryClickEvent event, Player player, Inventory top, Inventory clickedInv) {
        event.setCancelled(true);
        if (clickedInv != top) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName() == null ? "" : clicked.getItemMeta().getDisplayName();

        if (name.equals("§aEdit Kit Inventory Layouts")) {
            plugin.getGuiManager().openEditLayoutsGUI(player);
            return;
        }

        if (name.equals("§bAuto Fly")) {
            if (!player.hasPermission("duels.fly")) {
                player.sendMessage(plugin.getPrefix() + "§cYou don't have permission!");
                return;
            }

            boolean newValue = !plugin.getPlayerManager().getAutoFly(player.getUniqueId());
            plugin.getPlayerManager().setAutoFly(player.getUniqueId(), newValue);

            player.sendMessage(plugin.getPrefix() + "§7Auto Fly is now " + (newValue ? "§aENABLED" : "§cDISABLED"));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            plugin.getPlayerManager().applyLobbyFly(player);

            top.setItem(15, plugin.getGuiManager().createAutoFlyItem(player));
            return;
        }

        if (name.equals("§cClose")) {
            player.closeInventory();
            plugin.getGuiManager().closeGUI(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        // In Duel - Drag erlauben
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            return;
        }

        // Edit Layout GUI - only allow dragging into 0-35 of TOP inventory
        if (title.startsWith(GUIManager.EDIT_LAYOUT_GUI_TITLE_PREFIX)) {
            for (int rawSlot : event.getRawSlots()) {
                // rawSlot < topSize means it's in the top inventory
                if (rawSlot < event.getView().getTopInventory().getSize()) {
                    if (rawSlot >= 36) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            return;
        }

        // Block drags in other plugin GUIs
        if (title.equals(GUIManager.QUEUE_GUI_TITLE)
                || title.equals(GUIManager.BESTOF_GUI_TITLE)
                || title.equals(GUIManager.EDIT_LAYOUTS_GUI_TITLE)
                || title.equals(GUIManager.DUEL_GUI_TITLE)
                || title.equals(GUIManager.KITS_GUI_TITLE)
                || title.equals(GUIManager.SETTINGS_GUI_TITLE)
                || title.equals(GUIManager.STATS_GUI_TITLE)) {
            event.setCancelled(true);
        }
    }

    private String getEditLayoutKitId(Inventory top) {
        ItemStack info = top.getItem(45);
        if (info == null || !info.hasItemMeta()) return null;

        PersistentDataContainer pdc = info.getItemMeta().getPersistentDataContainer();
        String kitId = pdc.get(editKitKey, PersistentDataType.STRING);
        return (kitId == null || kitId.isEmpty()) ? null : kitId;
    }

}

