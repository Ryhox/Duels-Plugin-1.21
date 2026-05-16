package dev.duels.guis;

import dev.duels.DuelsPlugin;
import dev.duels.managers.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class GUIManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, GUI> openGUIs = new HashMap<>();
    private final NamespacedKey duelTargetKey;

    // GUI Titles
    public static final String QUEUE_GUI_TITLE = "§aSelect a Kit §8(Queue)";
    public static final String KITS_GUI_TITLE = "§bAvailable Kits";
    public static final String DUEL_GUI_TITLE = "§aSelect a Kit";
    public static final String BESTOF_GUI_TITLE = "§dSelect Match Length";
    public static final String SETTINGS_GUI_TITLE = "§cSettings";
    public static final String STATS_GUI_TITLE = "§bYour Stats";
    public static final String EDIT_LAYOUTS_GUI_TITLE = "§6Edit Kit Inventory Layouts";
    public static final String EDIT_LAYOUT_GUI_TITLE_PREFIX = "§eEdit Layout: ";

    // PDC keys
    private final NamespacedKey queueKitKey;
    private final NamespacedKey previewKitKey;
    private final NamespacedKey duelKitKey;
    private final NamespacedKey editKitKey;
    private final NamespacedKey bestOfValueKey;

    public GUIManager(DuelsPlugin plugin) {
        this.plugin = plugin;

        this.queueKitKey = new NamespacedKey(plugin, "queue_kit");
        this.previewKitKey = new NamespacedKey(plugin, "preview_kit");
        this.duelKitKey = new NamespacedKey(plugin, "duel_kit");
        this.editKitKey = new NamespacedKey(plugin, "edit_kit");
        this.bestOfValueKey = new NamespacedKey(plugin, "bestof_value");
        this.duelTargetKey = new NamespacedKey(plugin, "duel_target");

    }
    public static final String COMPARE_GUI_TITLE = "§bCompare Stats";

    private enum StatsSort {
        KILLS("Kills"),
        WINS("Wins"),
        KD("KD"),
        WINRATE("Winrate");

        private final String label;
        StatsSort(String label) { this.label = label; }
        public String label() { return label; }
    }

    private final Map<UUID, StatsSort> statsSortMode = new HashMap<>();

    public void openQueueGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, QUEUE_GUI_TITLE);
        populateQueueGUI(player, inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), new GUI(QUEUE_GUI_TITLE, System.currentTimeMillis()));
    }

    public void populateQueueGUI(Player viewer, Inventory inv) {
        inv.clear();

        Set<String> kits = plugin.getKitManager().getKitNames();
        if (kits.isEmpty()) {
            ItemStack noKits = createItem(Material.BARRIER, "§cNo Kits Available",
                    Arrays.asList("§7There are no kits available yet.", "§7Ask an admin to create some kits!"));
            inv.setItem(22, noKits);
            return;
        }

        List<String> sortedKits = new ArrayList<>(kits);
        sortedKits.sort(String::compareToIgnoreCase);

        int total = Math.min(sortedKits.size(), 35);
        int startRow = getCenteredStartRow(total, 7, 5);
        int rowsNeeded = (int) Math.ceil(total / 7.0);

        int kitIndex = 0;
        for (int r = 0; r < rowsNeeded; r++) {
            int remaining = total - kitIndex;
            int countThisRow = Math.min(7, remaining);

            int[] cols = centeredColsWithMiddleGap(countThisRow);

            for (int c = 0; c < countThisRow; c++) {
                String kitId = sortedKits.get(kitIndex);
                KitManager.Kit kit = plugin.getKitManager().getKit(kitId);

                Material previewMat = plugin.getKitManager().getKitPreviewMaterial(kitId);

                int queued = plugin.getQueueManager().getQueueSize(kitId);
                int playing = plugin.getQueueManager().getPlayingCount(kitId);
                int sum = queued + playing;
                boolean queuedByViewer = plugin.getQueueManager().isInQueue(viewer.getUniqueId(), kitId);

                ItemStack kitItem = new ItemStack(previewMat);
                kitItem.setAmount(Math.max(1, Math.min(64, sum)));

                ItemMeta meta = kitItem.getItemMeta();
                String display = (kit != null ? kit.getDisplayName() : kitId);
                meta.setDisplayName(display + (queuedByViewer ? " §7(Queued)" : ""));

                meta.setLore(Arrays.asList(
                        "§7In Queue: §a" + queued,
                        "§7Playing: §6" + playing,
                        "",
                        queuedByViewer ? "§cClick to leave queue" : "§eClick to join queue"
                ));

                meta.getPersistentDataContainer().set(queueKitKey, PersistentDataType.STRING, kitId);
                kitItem.setItemMeta(meta);

                int row = startRow + r;
                int col = cols[c];
                int slot = (row * 9) + col;
                inv.setItem(slot, kitItem);

                kitIndex++;
            }
        }

        ItemStack info = createItem(Material.PAPER, "§6Queue: Select a Kit",
                Arrays.asList("§7Pick a kit and you will be queued.", "§7Click again to leave.", "§7Live updates: queue + playing."));
        inv.setItem(4, info);

        ItemStack close = createItem(Material.BARRIER, "§cClose", null);
        inv.setItem(49, close);
    }

    private void populateKitsGUI(Inventory inv) {
        inv.clear();


        Set<String> kits = plugin.getKitManager().getKitNames();
        if (kits == null || kits.isEmpty()) {
            ItemStack noKits = createItem(Material.BARRIER, "§cNo Kits Available",
                    Arrays.asList("§7There are no kits available yet.", "§7Ask an admin to create some kits!"));
            inv.setItem(22, noKits);
            return;
        }


        List<String> sortedKits = new ArrayList<>(kits);
        sortedKits.sort(String::compareToIgnoreCase);


        int total = Math.min(sortedKits.size(), 35);
        int startRow = getCenteredStartRow(total, 7, 5);
        int rowsNeeded = (int) Math.ceil(total / 7.0);


        int kitIndex = 0;
        for (int r = 0; r < rowsNeeded; r++) {
            int remaining = total - kitIndex;
            int countThisRow = Math.min(7, remaining);


            int[] cols = centeredColsWithMiddleGap(countThisRow);


            for (int c = 0; c < countThisRow; c++) {
                String kitId = sortedKits.get(kitIndex++);
                Material previewMat = plugin.getKitManager().getKitPreviewMaterial(kitId);


                ItemStack kitItem = new ItemStack(previewMat);
                ItemMeta meta = kitItem.getItemMeta();


                KitManager.Kit kit = plugin.getKitManager().getKit(kitId);
                meta.setDisplayName(kit != null ? kit.getDisplayName() : kitId);


                int itemCount = kit != null ? kit.getItems().size() : 0;
                meta.setLore(Arrays.asList(
                        "§7Contains " + itemCount + " items",
                        "",
                        "§eClick to preview kit in your inventory"
                ));


                meta.getPersistentDataContainer().set(previewKitKey, PersistentDataType.STRING, kitId);
                kitItem.setItemMeta(meta);


                int row = startRow + r;
                int col = cols[c];
                inv.setItem((row * 9) + col, kitItem);
            }
        }


        ItemStack instructions = createItem(Material.PAPER, "§6Select a Kit",
                Arrays.asList("§7Click on a kit to preview it."));
        inv.setItem(4, instructions);


        ItemStack close = createItem(Material.BARRIER, "§cClose", null);
        inv.setItem(49, close);
    }

    public void openKitsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, KITS_GUI_TITLE);
        populateKitsGUI(inv);
        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), new GUI(KITS_GUI_TITLE, System.currentTimeMillis()));
    }



    public void openDuelGUI(Player sender, Player target) {
        Inventory inv = Bukkit.createInventory(null, 54, DUEL_GUI_TITLE);
        populateDuelGUI(sender, target, inv);
        sender.openInventory(inv);
        openGUIs.put(sender.getUniqueId(), new GUI(DUEL_GUI_TITLE, System.currentTimeMillis()));
    }

    private void populateDuelGUI(Player sender, Player target, Inventory inv) {
        inv.clear();

        Set<String> kits = plugin.getKitManager().getKitNames();
        if (kits.isEmpty()) {
            sender.sendMessage(plugin.getPrefix() + "§cNo kits available! Ask an admin to create one.");
            return;
        }

        List<String> sortedKits = new ArrayList<>(kits);
        sortedKits.sort(String::compareToIgnoreCase);

        int total = Math.min(sortedKits.size(), 35);
        int startRow = getCenteredStartRow(total, 7, 5);
        int rowsNeeded = (int) Math.ceil(total / 7.0);

        int kitIndex = 0;
        for (int r = 0; r < rowsNeeded; r++) {
            int remaining = total - kitIndex;
            int countThisRow = Math.min(7, remaining);

            int[] cols = centeredColsWithMiddleGap(countThisRow);

            for (int c = 0; c < countThisRow; c++) {
                String kitId = sortedKits.get(kitIndex);
                Material previewMat = plugin.getKitManager().getKitPreviewMaterial(kitId);

                ItemStack kitItem = new ItemStack(previewMat);
                ItemMeta meta = kitItem.getItemMeta();

                KitManager.Kit kit = plugin.getKitManager().getKit(kitId);
                String display = kit != null ? kit.getDisplayName() : kitId;
                meta.setDisplayName(display);

                int itemCount = kit != null ? kit.getItems().size() : 0;
                int defaultBestOf = plugin.getConfigManager().getMainConfig().getInt("default-bestof", 1);

                meta.setLore(Arrays.asList(
                        "§7Challenge §c" + target.getName() + " §7with this kit",
                        "§7Contains " + itemCount + " items",
                        "",
                        "§aLeft-Click §7= send request §8(Best of " + defaultBestOf + ")",
                        "§eRight-Click §7= choose Best-Of"
                ));

                meta.getPersistentDataContainer().set(duelKitKey, PersistentDataType.STRING, kitId);
                meta.getPersistentDataContainer().set(duelTargetKey, PersistentDataType.STRING, target.getUniqueId().toString());
                kitItem.setItemMeta(meta);

                int row = startRow + r;
                int col = cols[c];
                int slot = (row * 9) + col;
                inv.setItem(slot, kitItem);

                kitIndex++;
            }
        }

        ItemStack info = createItem(Material.PAPER, "§6Select a Kit",
                Arrays.asList("§7Choose a kit to challenge", "§7" + target.getName() + " with.", "", "§eLeft=send  §eRight=best-of"));

        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.getPersistentDataContainer().set(duelTargetKey, PersistentDataType.STRING, target.getUniqueId().toString());
        info.setItemMeta(infoMeta);

        inv.setItem(4, info);
    }

    public void openBestOfGUI(Player sender, Player target, String kitId) {
        Inventory inv = Bukkit.createInventory(null, 27, BESTOF_GUI_TITLE);

        KitManager.Kit kit = plugin.getKitManager().getKit(kitId);
        String display = kit != null ? kit.getDisplayName() : kitId;

        List<Integer> options = plugin.getConfigManager().getMainConfig().getIntegerList("bestof-options");
        if (options == null || options.isEmpty()) options = Arrays.asList(1, 3, 5);

        int slot = 10;
        for (int bestOf : options) {
            if (slot >= 17) break;

            ItemStack item = createItem(Material.PAPER, "§eBest of " + bestOf,
                    Arrays.asList("§7Kit: §b" + display, "§7Target: §c" + target.getName(), "", "§eClick to choose"));

            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(bestOfValueKey, PersistentDataType.INTEGER, bestOf);
            meta.getPersistentDataContainer().set(duelKitKey, PersistentDataType.STRING, kitId);
            meta.getPersistentDataContainer().set(duelTargetKey, PersistentDataType.STRING, target.getUniqueId().toString());
            item.setItemMeta(meta);


            inv.setItem(slot, item);
            slot++;
        }

        ItemStack back = createItem(Material.ARROW, "§cBack", null);
        inv.setItem(18, back);

        ItemStack close = createItem(Material.BARRIER, "§cClose", null);
        inv.setItem(26, close);

        sender.openInventory(inv);
        openGUIs.put(sender.getUniqueId(), new GUI(BESTOF_GUI_TITLE, System.currentTimeMillis()));
    }

    public void openSettingsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, SETTINGS_GUI_TITLE);

        ItemStack editLayouts = createItem(Material.CHEST, "§aEdit Kit Inventory Layouts",
                Arrays.asList("§7Edit your personal inventory layout", "§7for each kit separately.", "", "§eClick to edit"));
        inv.setItem(11, editLayouts);

        ItemStack autoFly = createAutoFlyItem(player);
        inv.setItem(15, autoFly);

        ItemStack close = createItem(Material.BARRIER, "§cClose", null);
        inv.setItem(26, close);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), new GUI(SETTINGS_GUI_TITLE, System.currentTimeMillis()));
    }

    public ItemStack createAutoFlyItem(Player player) {
        boolean enabled = plugin.getPlayerManager().getAutoFly(player.getUniqueId());
        Material material = enabled ? Material.LIME_DYE : Material.RED_DYE;

        return createItem(material, "§bAuto Fly",
                Arrays.asList("§7Automatically enable fly in lobby.", "",
                        "§7Status: " + (enabled ? "§aENABLED" : "§cDISABLED"), "", "§eClick to toggle"));
    }

    public void openEditLayoutsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, EDIT_LAYOUTS_GUI_TITLE);

        Set<String> kits = plugin.getKitManager().getKitNames();
        if (kits.isEmpty()) {
            ItemStack noKits = createItem(Material.BARRIER, "§cNo Kits Available",
                    Arrays.asList("§7There are no kits available yet.", "§7Ask an admin to create some kits!"));
            inv.setItem(22, noKits);
        } else {
            List<String> sortedKits = new ArrayList<>(kits);
            sortedKits.sort(String::compareToIgnoreCase);

            int total = Math.min(sortedKits.size(), 35);
            int startRow = getCenteredStartRow(total, 7, 5);
            int rowsNeeded = (int) Math.ceil(total / 7.0);

            int kitIndex = 0;
            for (int r = 0; r < rowsNeeded; r++) {
                int remaining = total - kitIndex;
                int countThisRow = Math.min(7, remaining);

                int[] cols = centeredColsWithMiddleGap(countThisRow);

                for (int c = 0; c < countThisRow; c++) {
                    String kitId = sortedKits.get(kitIndex);
                    KitManager.Kit kit = plugin.getKitManager().getKit(kitId);

                    Material previewMat = plugin.getKitManager().getKitPreviewMaterial(kitId);

                    ItemStack kitItem = new ItemStack(previewMat);
                    ItemMeta meta = kitItem.getItemMeta();

                    meta.setDisplayName(kit != null ? kit.getDisplayName() : kitId);

                    boolean hasCustomLayout = plugin.getKitManager().getCustomLayout(player.getUniqueId(), kitId) != null;

                    meta.getPersistentDataContainer().set(editKitKey, PersistentDataType.STRING, kitId);

                    meta.setLore(Arrays.asList(
                            "§7Edit your inventory layout for this kit.",
                            "",
                            hasCustomLayout ? "§a✓ Custom layout saved" : "§7No custom layout yet",
                            "",
                            "§eClick to edit layout"
                    ));

                    kitItem.setItemMeta(meta);

                    int row = startRow + r;
                    int col = cols[c];
                    int slot = (row * 9) + col;
                    inv.setItem(slot, kitItem);

                    kitIndex++;
                }
            }
        }

        ItemStack instructions = createItem(Material.PAPER, "§6Select a Kit to Edit",
                Arrays.asList("§7Click on a kit to §eedit §7your", "§apersonal inventory layout §7for it."));
        inv.setItem(4, instructions);

        ItemStack close = createItem(Material.BARRIER, "§cClose", null);
        inv.setItem(49, close);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), new GUI(EDIT_LAYOUTS_GUI_TITLE, System.currentTimeMillis()));
    }

    public void openEditLayoutGUI(Player player, String kitId) {
        KitManager.Kit kit = plugin.getKitManager().getKit(kitId);
        String display = kit != null ? kit.getDisplayName() : kitId;

        Inventory inv = Bukkit.createInventory(null, 54, EDIT_LAYOUT_GUI_TITLE_PREFIX + display);

        ItemStack[] customLayout = plugin.getKitManager().getCustomLayout(player.getUniqueId(), kitId);

        // WICHTIG: Wenn CustomLayout existiert -> NUR custom benutzen, kein Fallback aufs Kit
        if (customLayout != null) {
            for (int i = 0; i < 36; i++) {
                ItemStack it = (i < customLayout.length ? customLayout[i] : null);
                inv.setItem(i, it == null ? null : it.clone());
            }
        } else {
            // Nur wenn noch kein CustomLayout existiert -> Kit Default reinladen
            for (int i = 0; i < 36; i++) {
                ItemStack it = (kit != null ? kit.getItem(i) : null);
                inv.setItem(i, it == null ? null : it.clone());
            }
        }

        // --- Armor/Offhand usw. bleibt wie bei dir ---
        ItemStack redGlass = createItem(Material.RED_STAINED_GLASS_PANE, "§cFixed Slot",
                Arrays.asList("§7This slot is fixed by the kit.", "§7You cannot change armor/offhand layout."));

        inv.setItem(36, kit != null && kit.getItem(100) != null ? kit.getItem(100).clone() : redGlass.clone());
        inv.setItem(37, kit != null && kit.getItem(101) != null ? kit.getItem(101).clone() : redGlass.clone());
        inv.setItem(38, kit != null && kit.getItem(102) != null ? kit.getItem(102).clone() : redGlass.clone());
        inv.setItem(39, kit != null && kit.getItem(103) != null ? kit.getItem(103).clone() : redGlass.clone());
        inv.setItem(40, kit != null && kit.getItem(99)  != null ? kit.getItem(99).clone()  : redGlass.clone());

        // Info Paper muss da bleiben wo du es willst (45)
        ItemStack info = createItem(Material.PAPER, "§6Editing Layout",
                Arrays.asList("§7Move items in slots §a0-35§7.", "§7Armor/offhand and buttons are locked.", "", "§eClick Save when done."));
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.getPersistentDataContainer().set(editKitKey, PersistentDataType.STRING, kitId);
        info.setItemMeta(infoMeta);
        inv.setItem(45, info);

        ItemStack reset = createItem(Material.RED_DYE, "§cReset to Default",
                Arrays.asList("§7Reset your inventory layout", "§7back to the default arrangement."));
        inv.setItem(51, tagKitId(reset, kitId));

        ItemStack save = createItem(Material.LIME_DYE, "§aSave Layout",
                Arrays.asList("§7Save your current inventory arrangement", "§7as your personal layout for this kit."));
        inv.setItem(52, tagKitId(save, kitId));

        ItemStack close = createItem(Material.BARRIER, "§cClose", Arrays.asList("§7Close without saving"));
        inv.setItem(53, tagKitId(close, kitId));

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), new GUI(EDIT_LAYOUT_GUI_TITLE_PREFIX + display, System.currentTimeMillis()));
    }

    private ItemStack tagKitId(ItemStack item, String kitId) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(editKitKey, PersistentDataType.STRING, kitId);
        item.setItemMeta(meta);
        return item;
    }

    public void openStatsGUI(Player player) {
        StatsSort sort = statsSortMode.getOrDefault(player.getUniqueId(), StatsSort.KILLS);

        Inventory inv = Bukkit.createInventory(null, 27, STATS_GUI_TITLE);

        // Center: your head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(player);
        headMeta.setDisplayName("§a" + player.getName());

        int kills = plugin.getPlayerManager().getStat(player.getUniqueId(), "kills");
        int deaths = plugin.getPlayerManager().getStat(player.getUniqueId(), "deaths");
        int wins = plugin.getPlayerManager().getStat(player.getUniqueId(), "wins");
        int losses = plugin.getPlayerManager().getStat(player.getUniqueId(), "losses");
        double kd = deaths == 0 ? kills : (double) kills / deaths;
        String winrate = plugin.getPlayerManager().calculateWinrate(wins, losses);

        headMeta.setLore(Arrays.asList(
                "§7Kills: §b" + kills,
                "§7Deaths: §c" + deaths,
                "§7KD Ratio: §6" + String.format("%.2f", kd),
                "§7Wins: §a" + wins,
                "§7Losses: §c" + losses,
                "§7Win Rate: §b" + winrate
        ));
        head.setItemMeta(headMeta);
        inv.setItem(13, head);

        // Slot 11: Top 5 paper
        inv.setItem(11, createTop5Item(player, sort));

        // Slot 15: Search Players (sign)
        ItemStack search = createItem(Material.OAK_SIGN, "§eSearch Players",
                Arrays.asList("§7Search a player by name", "§7and compare stats.", "", "§eClick to search"));
        inv.setItem(15, search);

        // Close
        ItemStack close = createItem(Material.BARRIER, "§cClose", null);
        inv.setItem(26, close);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), new GUI(STATS_GUI_TITLE, System.currentTimeMillis()));
    }
    private ItemStack createTop5Item(Player viewer, StatsSort sort) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Sorted: §b" + sort.label() + " §7(click to change)");
        lore.add("");

        List<dev.duels.objects.PlayerData> all = plugin.getPlayerManager().getAllPlayerDataSnapshot();

        all.sort((a, b) -> {
            double av = getSortValue(a, sort);
            double bv = getSortValue(b, sort);
            int cmp = Double.compare(bv, av); // desc
            if (cmp != 0) return cmp;

            String an = a.getName() == null ? "" : a.getName();
            String bn = b.getName() == null ? "" : b.getName();
            return an.compareToIgnoreCase(bn);
        });

        int shown = 0;
        for (dev.duels.objects.PlayerData pd : all) {
            if (pd == null) continue;
            String name = pd.getName() == null ? "Unknown" : pd.getName();

            // skip unknown names if you want
            if (name.equalsIgnoreCase("Unknown")) continue;

            shown++;
            lore.add("§7#" + shown + ": §f" + name + " §8- §a" + formatSortValue(pd, sort));
            if (shown >= 5) break;
        }

        if (shown == 0) {
            lore.add("§7No data yet.");
        }

        return createItem(Material.PAPER, "§6Top 5 Players", lore);
    }

    private double getSortValue(dev.duels.objects.PlayerData pd, StatsSort sort) {
        int kills = pd.getKills();
        int deaths = pd.getDeaths();
        int wins = pd.getWins();
        int losses = pd.getLosses();

        return switch (sort) {
            case KILLS -> kills;
            case WINS -> wins;
            case KD -> deaths == 0 ? kills : (double) kills / deaths;
            case WINRATE -> {
                int total = wins + losses;
                yield total == 0 ? 0.0 : ((double) wins / total) * 100.0;
            }
        };
    }

    private String formatSortValue(dev.duels.objects.PlayerData pd, StatsSort sort) {
        int kills = pd.getKills();
        int deaths = pd.getDeaths();
        int wins = pd.getWins();
        int losses = pd.getLosses();

        return switch (sort) {
            case KILLS -> String.valueOf(kills);
            case WINS -> String.valueOf(wins);
            case KD -> String.format("%.2f", deaths == 0 ? (double) kills : (double) kills / deaths);
            case WINRATE -> {
                int total = wins + losses;
                double wr = total == 0 ? 0.0 : ((double) wins / total) * 100.0;
                yield String.format("%.1f%%", wr);
            }
        };
    }

    private StatsSort nextSort(StatsSort current) {
        StatsSort[] vals = StatsSort.values();
        int i = (current.ordinal() + 1) % vals.length;
        return vals[i];
    }

    public void openCompareGUI(Player viewer, UUID targetUuid) {
        Player target = Bukkit.getPlayer(targetUuid);
        String targetName = (target != null ? target.getName() : plugin.getPlayerManager().getPlayerData(targetUuid).getName());
        if (targetName == null) targetName = "Unknown";

        Inventory inv = Bukkit.createInventory(null, 27, COMPARE_GUI_TITLE);

        ItemStack yours = buildCompareHead(viewer.getUniqueId(), viewer.getName(), true);
        ItemStack theirs = buildCompareHead(targetUuid, targetName, false);

        inv.setItem(11, yours);
        inv.setItem(13, createItem(Material.PAPER, "§bComparison",
                Arrays.asList("§7Left: §aYou", "§7Right: §c" + targetName)));
        inv.setItem(15, theirs);

        inv.setItem(26, createItem(Material.BARRIER, "§cClose", null));

        viewer.openInventory(inv);
        openGUIs.put(viewer.getUniqueId(), new GUI(COMPARE_GUI_TITLE, System.currentTimeMillis()));
    }

    private ItemStack buildCompareHead(UUID uuid, String name, boolean self) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        Player online = Bukkit.getPlayer(uuid);
        if (online != null) meta.setOwningPlayer(online);

        meta.setDisplayName((self ? "§a" : "§c") + name);

        int kills = plugin.getPlayerManager().getStat(uuid, "kills");
        int deaths = plugin.getPlayerManager().getStat(uuid, "deaths");
        int wins = plugin.getPlayerManager().getStat(uuid, "wins");
        int losses = plugin.getPlayerManager().getStat(uuid, "losses");
        double kd = deaths == 0 ? kills : (double) kills / deaths;
        String winrate = plugin.getPlayerManager().calculateWinrate(wins, losses);

        meta.setLore(Arrays.asList(
                "§7Kills: §b" + kills,
                "§7Deaths: §c" + deaths,
                "§7KD: §6" + String.format("%.2f", kd),
                "§7Wins: §a" + wins,
                "§7Losses: §c" + losses,
                "§7Winrate: §b" + winrate
        ));

        head.setItemMeta(meta);
        return head;
    }
    public void cycleStatsSortAndReopen(Player player) {
        UUID uuid = player.getUniqueId();
        StatsSort current = statsSortMode.getOrDefault(uuid, StatsSort.KILLS);
        StatsSort next = nextSort(current);
        statsSortMode.put(uuid, next);

        openStatsGUI(player);
    }



    public void updateOpenGUIs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() == null) continue;

            String titleStripped = ChatColor.stripColor(player.getOpenInventory().getTitle());
            GUI gui = openGUIs.get(player.getUniqueId());

            if (gui != null && System.currentTimeMillis() - gui.getOpenTime() > 1000) {
                if (titleStripped.equals(ChatColor.stripColor(QUEUE_GUI_TITLE))) {
                    populateQueueGUI(player, player.getOpenInventory().getTopInventory());
                }
            }
        }
    }

    public void refreshQueueGUIs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() == null) continue;

            String title = player.getOpenInventory().getTitle();
            if (title.equals(QUEUE_GUI_TITLE)) {
                populateQueueGUI(player, player.getOpenInventory().getTopInventory());
            }
        }
    }

    public void closeGUI(UUID playerId) {
        openGUIs.remove(playerId);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int[] centeredColsWithMiddleGap(int n) {
        int center = 4;
        int[] cols = new int[n];
        if (n <= 0) return cols;

        if (n % 2 == 1) {
            int start = center - (n / 2);
            for (int i = 0; i < n; i++) cols[i] = start + i;
        } else {
            int half = n / 2;
            int idx = 0;
            for (int i = half; i >= 1; i--) cols[idx++] = center - i;
            for (int i = 1; i <= half; i++) cols[idx++] = center + i;
        }
        return cols;
    }

    private int getCenteredStartRow(int totalItems, int perRow, int usableRows) {
        int rowsNeeded = (int) Math.ceil(totalItems / (double) perRow);
        return Math.max(0, (usableRows - rowsNeeded) / 2);
    }

    private static class GUI {
        private final String title;
        private final long openTime;

        public GUI(String title, long openTime) {
            this.title = title;
            this.openTime = openTime;
        }

        public String getTitle() { return title; }
        public long getOpenTime() { return openTime; }
    }
}
