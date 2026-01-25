package dev.duels.managers;

import dev.duels.DuelsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class KitManager {

    private final DuelsPlugin plugin;
    private final Map<String, Kit> kits = new HashMap<>(); // key = kitId (clean)
    private final Map<UUID, Map<String, ItemStack[]>> playerKitLayouts = new HashMap<>();

    public KitManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadKits() {
        kits.clear();

        if (!plugin.getConfigManager().getKitsConfig().contains("kits")) {
            plugin.getLogger().info("Loaded 0 kits");
            return;
        }

        ConfigurationSection kitsSection = plugin.getConfigManager().getKitsConfig().getConfigurationSection("kits");
        if (kitsSection == null) {
            plugin.getLogger().info("Loaded 0 kits");
            return;
        }

        for (String kitId : kitsSection.getKeys(false)) {
            Kit kit = new Kit(kitId);

            // Display name (colored). Fallback for older configs: ".name"
            String storedDisplay = kitsSection.getString(kitId + ".displayName", null);
            if (storedDisplay == null) {
                storedDisplay = kitsSection.getString(kitId + ".name", kitId);
            }
            kit.setDisplayName(colorize(storedDisplay));

            // Preview Material
            String previewMatStr = kitsSection.getString(kitId + ".preview_material", "DIAMOND_SWORD");
            try {
                kit.setPreviewMaterial(Material.valueOf(previewMatStr));
            } catch (IllegalArgumentException e) {
                kit.setPreviewMaterial(Material.DIAMOND_SWORD);
            }

            // Items laden
            if (kitsSection.contains(kitId + ".items")) {
                ConfigurationSection itemsSection = kitsSection.getConfigurationSection(kitId + ".items");
                if (itemsSection != null) {
                    for (String slotStr : itemsSection.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            ItemStack item = itemsSection.getItemStack(slotStr);
                            if (item != null && item.getType() != Material.AIR) {
                                kit.setItem(slot, item);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            kits.put(kitId, kit);
        }

        plugin.getLogger().info("Loaded " + kits.size() + " kits");
    }

    public void saveKit(String kitId, Kit kit) {
        String path = "kits." + kitId;

        // keep legacy field if you want (optional), but the important one is displayName
        plugin.getConfigManager().getKitsConfig().set(path + ".name", kitId);

        // store displayName with & codes (clean YAML)
        plugin.getConfigManager().getKitsConfig().set(path + ".displayName", uncolorize(kit.getDisplayName()));

        plugin.getConfigManager().getKitsConfig().set(path + ".preview_material", kit.getPreviewMaterial().name());

        // Alte Items löschen
        plugin.getConfigManager().getKitsConfig().set(path + ".items", null);

        // Neue Items speichern
        for (Map.Entry<Integer, ItemStack> entry : kit.getItems().entrySet()) {
            plugin.getConfigManager().getKitsConfig().set(path + ".items." + entry.getKey(), entry.getValue());
        }

        plugin.getConfigManager().saveKitsConfig();
        kits.put(kitId, kit);
    }

    public void deleteKit(String kitId) {
        kits.remove(kitId);
        plugin.getConfigManager().getKitsConfig().set("kits." + kitId, null);
        plugin.getConfigManager().saveKitsConfig();
    }

    public void giveKit(Player player, String kitId) {
        Kit kit = kits.get(kitId);
        if (kit == null) return;

        PlayerInventory inv = player.getInventory();
        inv.clear();

        ItemStack[] customLayout = getCustomLayout(player.getUniqueId(), kitId);

        if (customLayout != null) {
            for (int i = 0; i < Math.min(customLayout.length, 36); i++) {
                if (customLayout[i] != null && customLayout[i].getType() != Material.AIR) {
                    inv.setItem(i, customLayout[i].clone());
                }
            }
        } else {
            for (Map.Entry<Integer, ItemStack> entry : kit.getItems().entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < 36) {
                    inv.setItem(slot, item.clone());
                }
            }
        }

        inv.setBoots(kit.getItem(100) != null ? kit.getItem(100).clone() : null);
        inv.setLeggings(kit.getItem(101) != null ? kit.getItem(101).clone() : null);
        inv.setChestplate(kit.getItem(102) != null ? kit.getItem(102).clone() : null);
        inv.setHelmet(kit.getItem(103) != null ? kit.getItem(103).clone() : null);

        inv.setItemInOffHand(kit.getItem(99) != null ? kit.getItem(99).clone() : null);

        player.updateInventory();
    }

    public void giveKitPreview(Player player, String kitId) {
        Kit kit = kits.get(kitId);
        if (kit == null) {
            player.sendMessage(plugin.getPrefix() + "§cKit not found: " + kitId);
            return;
        }

        ItemStack[] saved = player.getInventory().getContents();
        ItemStack[] savedArmor = player.getInventory().getArmorContents();
        ItemStack savedOffhand = player.getInventory().getItemInOffHand();

        giveKit(player, kitId);

        player.sendMessage(plugin.getPrefix() + "§aPreviewing kit: " + kit.getDisplayName());
        player.sendMessage(plugin.getPrefix() + "§7Use §c/spawn §7to return to your normal inventory.");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                player.getInventory().setContents(saved);
                player.getInventory().setArmorContents(savedArmor);
                player.getInventory().setItemInOffHand(savedOffhand);
                player.updateInventory();
                player.sendMessage(plugin.getPrefix() + "§7Your inventory has been restored.");
            }
        }, 20L * 60L * 5L);
    }

    public void saveCustomLayout(UUID playerId, String kitId, ItemStack[] layout) {
        Map<String, ItemStack[]> layouts = playerKitLayouts.computeIfAbsent(playerId, k -> new HashMap<>());
        layouts.put(kitId, layout);

        String path = playerId.toString() + ".kitLayouts." + kitId;
        plugin.getConfigManager().getPlayersConfig().set(path, null);

        for (int i = 0; i < layout.length; i++) {
            if (layout[i] != null && layout[i].getType() != Material.AIR) {
                plugin.getConfigManager().getPlayersConfig().set(path + "." + i, layout[i]);
            }
        }

        plugin.getConfigManager().savePlayersConfig();
    }

    public ItemStack[] getCustomLayout(UUID playerId, String kitId) {
        Map<String, ItemStack[]> layouts = playerKitLayouts.get(playerId);
        if (layouts == null) return null;
        return layouts.get(kitId);
    }

    public void deleteCustomLayout(UUID playerId, String kitId) {
        Map<String, ItemStack[]> layouts = playerKitLayouts.get(playerId);
        if (layouts != null) {
            layouts.remove(kitId);
            plugin.getConfigManager().getPlayersConfig().set(playerId.toString() + ".kitLayouts." + kitId, null);
            plugin.getConfigManager().savePlayersConfig();
        }
    }

    public void loadPlayerKitLayouts() {
        playerKitLayouts.clear();

        for (String playerIdStr : plugin.getConfigManager().getPlayersConfig().getKeys(false)) {
            if (!isValidUUID(playerIdStr)) continue;

            UUID playerId = UUID.fromString(playerIdStr);
            String path = playerIdStr + ".kitLayouts";

            if (!plugin.getConfigManager().getPlayersConfig().contains(path)) continue;

            ConfigurationSection layoutsSection = plugin.getConfigManager().getPlayersConfig().getConfigurationSection(path);
            if (layoutsSection == null) continue;

            Map<String, ItemStack[]> layouts = new HashMap<>();

            for (String kitId : layoutsSection.getKeys(false)) {
                ItemStack[] layout = new ItemStack[36];
                ConfigurationSection kitSection = layoutsSection.getConfigurationSection(kitId);
                if (kitSection == null) continue;

                for (String slotStr : kitSection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        if (slot >= 0 && slot < 36) {
                            layout[slot] = kitSection.getItemStack(slotStr);
                        }
                    } catch (NumberFormatException ignored) {}
                }

                layouts.put(kitId, layout);
            }

            playerKitLayouts.put(playerId, layouts);
        }
    }

    public boolean kitExists(String kitId) {
        return kits.containsKey(kitId);
    }

    public Set<String> getKitNames() {
        return kits.keySet();
    }

    public Kit getKit(String kitId) {
        return kits.get(kitId);
    }

    public Material getKitPreviewMaterial(String kitId) {
        Kit kit = kits.get(kitId);
        return kit != null ? kit.getPreviewMaterial() : Material.DIAMOND_SWORD;
    }

    private boolean isValidUUID(String string) {
        try {
            UUID.fromString(string);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String colorize(String s) {
        if (s == null) return null;
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static String uncolorize(String s) {
        if (s == null) return null;
        // store with '&' codes so YAML stays readable
        return s.replace('§', '&');
    }

    public static class Kit {
        private final String id; // clean id / config key
        private String displayName; // colored
        private Material previewMaterial;
        private final Map<Integer, ItemStack> items = new HashMap<>();

        public Kit(String id) {
            this.id = id;
            this.previewMaterial = Material.DIAMOND_SWORD;
        }

        public String getId() { return id; }

        public String getDisplayName() {
            return displayName != null ? displayName : id;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Material getPreviewMaterial() { return previewMaterial; }
        public void setPreviewMaterial(Material material) { this.previewMaterial = material; }
        public Map<Integer, ItemStack> getItems() { return items; }

        public void setItem(int slot, ItemStack item) {
            items.put(slot, item);
        }

        public ItemStack getItem(int slot) {
            return items.get(slot);
        }
    }
    public String getKitDisplayName(String kitId) {
        Kit kit = kits.get(kitId);
        if (kit == null) return "§e§l" + kitId; // fallback falls kit fehlt
        return kit.getDisplayName(); // ist schon colored
    }

    public void loadCustomLayoutsFromFile() {
        playerKitLayouts.clear();

        // make sure players.yml is loaded from disk
        plugin.getConfigManager().reloadPlayersConfig();

        var cfg = plugin.getConfigManager().getPlayersConfig();

        // UUIDs at root
        for (String playerIdStr : cfg.getKeys(false)) {
            if (!isValidUUID(playerIdStr)) continue;

            UUID playerId = UUID.fromString(playerIdStr);
            String path = playerIdStr + ".kitLayouts";

            ConfigurationSection layoutsSection = cfg.getConfigurationSection(path);
            if (layoutsSection == null) continue;

            Map<String, ItemStack[]> layouts = new HashMap<>();

            for (String kitId : layoutsSection.getKeys(false)) {
                ConfigurationSection kitSection = layoutsSection.getConfigurationSection(kitId);
                if (kitSection == null) continue;

                ItemStack[] layout = new ItemStack[36];

                for (String slotStr : kitSection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        if (slot < 0 || slot >= 36) continue;

                        ItemStack it = kitSection.getItemStack(slotStr);
                        layout[slot] = (it == null || it.getType() == Material.AIR) ? null : it.clone();
                    } catch (NumberFormatException ignored) {}
                }

                // only store if something is inside
                boolean any = false;
                for (ItemStack it : layout) {
                    if (it != null && it.getType() != Material.AIR) { any = true; break; }
                }
                if (any) layouts.put(kitId, layout);
            }

            if (!layouts.isEmpty()) {
                playerKitLayouts.put(playerId, layouts);
            }
        }
    }



}
