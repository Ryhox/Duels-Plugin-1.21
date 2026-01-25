package dev.duels.commands;

import dev.duels.DuelsPlugin;
import dev.duels.managers.KitManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Set;

public class KitCommand implements CommandExecutor {

    private final DuelsPlugin plugin;

    public KitCommand(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + "§cOnly players can use this command!");
            return true;
        }

        if (!player.hasPermission("duels.admin")) {
            player.sendMessage(plugin.getPrefix() + "§cYou don't have permission!");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("add")) {
            return handleAdd(player, args);
        }

        if (sub.equals("remove") || sub.equals("delete")) {
            return handleRemove(player, args);
        }

        sendUsage(player);
        return true;
    }

    private boolean handleAdd(Player player, String[] args) {
        // /kit add <name with spaces and &> <material>
        if (args.length < 3) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /kit add <name> <preview_item>");
            player.sendMessage(plugin.getPrefix() + "§7Example: /kit add &a&lSword DIAMOND_SWORD");
            return true;
        }

        String previewItemStr = args[args.length - 1].toUpperCase(Locale.ROOT);
        Material previewMaterial;
        try {
            previewMaterial = Material.valueOf(previewItemStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getPrefix() + "§cInvalid material! Use something like DIAMOND_SWORD, BOW, etc.");
            return true;
        }

        String rawName = join(args, 1, args.length - 1); // join args[1..len-2]
        String displayName = ChatColor.translateAlternateColorCodes('&', rawName);

        String plainName = ChatColor.stripColor(displayName);
        if (plainName == null) plainName = "";
        plainName = plainName.trim();

        if (plainName.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + "§cInvalid kit name!");
            return true;
        }

        String kitId = toKitId(plainName);

        if (plugin.getKitManager().kitExists(kitId)) {
            KitManager.Kit existing = plugin.getKitManager().getKit(kitId);
            String existingDisplay = existing != null ? existing.getDisplayName() : kitId;
            player.sendMessage(plugin.getPrefix() + "§cKit already exists: " + existingDisplay + " §8(§f" + kitId + "§8)");
            player.sendMessage(plugin.getPrefix() + "§7Use §c/kit remove " + plainName + " §7first.");
            return true;
        }

        KitManager.Kit kit = new KitManager.Kit(kitId);
        kit.setDisplayName(displayName);
        kit.setPreviewMaterial(previewMaterial);

        // Main inventory (0-35)
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < 36; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                kit.setItem(i, contents[i].clone());
            }
        }

        // Armor (100-103)
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && armor[i].getType() != Material.AIR) {
                kit.setItem(100 + i, armor[i].clone());
            }
        }

        // Offhand (99)
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() != Material.AIR) {
            kit.setItem(99, offhand.clone());
        }

        plugin.getKitManager().saveKit(kitId, kit);

        player.sendMessage(plugin.getPrefix() + "§7Kit " + kit.getDisplayName() + " §7has been §a§lcreated!");
        player.sendMessage(plugin.getPrefix() + "§7Internal name: §f" + kitId);
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        // /kit remove <name with spaces (can include colors, ignored)>
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + "§7Usage: /kit remove <name>");
            player.sendMessage(plugin.getPrefix() + "§7Example: /kit remove No Debuff Kit");
            return true;
        }

        String raw = join(args, 1, args.length); // all after "remove"
        String colored = ChatColor.translateAlternateColorCodes('&', raw);
        String plain = ChatColor.stripColor(colored);
        if (plain == null) plain = "";
        plain = plain.trim();

        if (plain.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + "§cInvalid kit name!");
            return true;
        }

        String kitId = toKitId(plain);

        // if user typed exact kitId, allow it too
        if (!plugin.getKitManager().kitExists(kitId)) {
            // try direct match by iterating displayNames (optional but helpful)
            String resolved = resolveKitIdByDisplayOrId(raw);
            if (resolved != null) kitId = resolved;
        }

        if (!plugin.getKitManager().kitExists(kitId)) {
            player.sendMessage(plugin.getPrefix() + "§cKit not found: §f" + plain);
            return true;
        }

        KitManager.Kit kit = plugin.getKitManager().getKit(kitId);
        String display = kit != null ? kit.getDisplayName() : kitId;

        plugin.getKitManager().deleteKit(kitId);

        player.sendMessage(plugin.getPrefix() + "§7Kit " + display + " §7has been §c§lremoved!");
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(plugin.getPrefix() + "§7Usage:");
        player.sendMessage(plugin.getPrefix() + "§7- /kit add <name> <preview_item>");
        player.sendMessage(plugin.getPrefix() + "§7- /kit remove <name>");
        player.sendMessage(plugin.getPrefix() + "§7Examples:");
        player.sendMessage(plugin.getPrefix() + "§7- /kit add &a&lSword DIAMOND_SWORD");
        player.sendMessage(plugin.getPrefix() + "§7- /kit add &bNo Debuff Kit NETHERITE_SWORD");
        player.sendMessage(plugin.getPrefix() + "§7- /kit remove No Debuff Kit");
    }

    private static String join(String[] args, int fromInclusive, int toExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (i > fromInclusive) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static String toKitId(String plainName) {
        // stable id: lowercase, spaces to _, remove weird chars
        String id = plainName.toLowerCase(Locale.ROOT).replace(' ', '_');
        id = id.replaceAll("[^a-z0-9_\\-]", ""); // keep a-z 0-9 _ -
        while (id.contains("__")) id = id.replace("__", "_");
        if (id.startsWith("_")) id = id.substring(1);
        if (id.endsWith("_")) id = id.substring(0, id.length() - 1);
        return id.isEmpty() ? "kit" : id;
    }

    private String resolveKitIdByDisplayOrId(String input) {
        String inputColored = ChatColor.translateAlternateColorCodes('&', input);
        String inputPlain = ChatColor.stripColor(inputColored);
        if (inputPlain == null) inputPlain = "";
        inputPlain = inputPlain.trim();

        Set<String> ids = plugin.getKitManager().getKitNames();
        for (String id : ids) {
            if (id.equalsIgnoreCase(input.trim())) return id;

            KitManager.Kit kit = plugin.getKitManager().getKit(id);
            if (kit == null) continue;

            String kitPlain = ChatColor.stripColor(kit.getDisplayName());
            if (kitPlain == null) kitPlain = "";
            if (kitPlain.trim().equalsIgnoreCase(inputPlain)) return id;
        }
        return null;
    }
}
