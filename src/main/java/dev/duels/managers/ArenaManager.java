package dev.duels.managers;

import dev.duels.DuelsPlugin;
import dev.duels.objects.Arena;
import dev.duels.objects.BlockVector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.*;

public class ArenaManager {

    private final DuelsPlugin plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final Map<String, Arena> availableArenas = new HashMap<>();
    private Location spawnLocation;

    public ArenaManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadArenas() {
        arenas.clear();
        availableArenas.clear();

        // Lobby spawn from main config (can stay as Bukkit Location)
        if (plugin.getConfigManager().getMainConfig().contains("spawn")) {
            spawnLocation = plugin.getConfigManager().getMainConfig().getLocation("spawn");
        }

        var arenaCfg = plugin.getConfigManager().getArenaConfig();
        if (arenaCfg == null || !arenaCfg.contains("arenas")) return;

        var arenasSec = arenaCfg.getConfigurationSection("arenas");
        if (arenasSec == null) return;

        for (String arenaName : arenasSec.getKeys(false)) {
            Arena arena = new Arena(arenaName);
            String path = "arenas." + arenaName;

            // --- Load positions (supports BOTH formats) ---
            // Format A (old): Bukkit serialized Location section
            // Format B (new/pretty): "world,x,y,z,yaw,pitch" string

            // spawn1
            if (arenaCfg.isString(path + ".spawn1")) {
                arena.setSpawn1(stringToLoc(arenaCfg.getString(path + ".spawn1")));
            } else if (arenaCfg.contains(path + ".spawn1")) {
                arena.setSpawn1(arenaCfg.getLocation(path + ".spawn1"));
            }

            // spawn2
            if (arenaCfg.isString(path + ".spawn2")) {
                arena.setSpawn2(stringToLoc(arenaCfg.getString(path + ".spawn2")));
            } else if (arenaCfg.contains(path + ".spawn2")) {
                arena.setSpawn2(arenaCfg.getLocation(path + ".spawn2"));
            }

            // corner1
            if (arenaCfg.isString(path + ".corner1")) {
                arena.setCorner1(stringToLoc(arenaCfg.getString(path + ".corner1")));
            } else if (arenaCfg.contains(path + ".corner1")) {
                arena.setCorner1(arenaCfg.getLocation(path + ".corner1"));
            }

            // corner2
            if (arenaCfg.isString(path + ".corner2")) {
                arena.setCorner2(stringToLoc(arenaCfg.getString(path + ".corner2")));
            } else if (arenaCfg.contains(path + ".corner2")) {
                arena.setCorner2(arenaCfg.getLocation(path + ".corner2"));
            }

            // Snapshot load (your existing method)
            loadArenaSnapshot(arena);

            arenas.put(arenaName, arena);
            availableArenas.put(arenaName, arena);

            plugin.getLogger().info("Loaded arena: " + arenaName
                    + " (spawns=" + (arena.getSpawn1() != null && arena.getSpawn2() != null)
                    + ", corners=" + (arena.getCorner1() != null && arena.getCorner2() != null)
                    + ", snapshot=" + arena.hasSnapshot() + ")");
        }
    }

    private void loadArenaSnapshot(Arena arena) {
        String path = "arenas." + arena.getName() + ".snapshot";
        if (!plugin.getConfigManager().getArenaConfig().contains(path + ".blocks")) return;

        String worldName = plugin.getConfigManager().getArenaConfig().getString(path + ".world");
        if (worldName == null) return;

        arena.setSnapshotWorld(worldName);

        int minX = plugin.getConfigManager().getArenaConfig().getInt(path + ".minX");
        int minY = plugin.getConfigManager().getArenaConfig().getInt(path + ".minY");
        int minZ = plugin.getConfigManager().getArenaConfig().getInt(path + ".minZ");
        int maxX = plugin.getConfigManager().getArenaConfig().getInt(path + ".maxX");
        int maxY = plugin.getConfigManager().getArenaConfig().getInt(path + ".maxY");
        int maxZ = plugin.getConfigManager().getArenaConfig().getInt(path + ".maxZ");

        arena.setSnapshotBounds(minX, minY, minZ, maxX, maxY, maxZ);
        arena.getOriginalBlocks().clear();

        List<String> list = plugin.getConfigManager().getArenaConfig().getStringList(path + ".blocks");
        for (String line : list) {
            String[] parts = line.split("\\|", 2);
            if (parts.length != 2) continue;

            String[] xyz = parts[0].split(",", 3);
            if (xyz.length != 3) continue;

            try {
                int x = Integer.parseInt(xyz[0]);
                int y = Integer.parseInt(xyz[1]);
                int z = Integer.parseInt(xyz[2]);

                BlockData bd = Bukkit.createBlockData(parts[1]);
                arena.getOriginalBlocks().put(new BlockVector(x, y, z), bd);
            } catch (Exception ignored) {}
        }
    }
    private String locToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.getWorld().getName() + ","
                + loc.getX() + "," + loc.getY() + "," + loc.getZ() + ","
                + loc.getYaw() + "," + loc.getPitch();
    }

    private Location stringToLoc(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] p = s.split(",", 6);
        if (p.length < 4) return null;

        var world = Bukkit.getWorld(p[0]);
        if (world == null) return null;

        double x = Double.parseDouble(p[1]);
        double y = Double.parseDouble(p[2]);
        double z = Double.parseDouble(p[3]);

        float yaw = (p.length >= 5) ? Float.parseFloat(p[4]) : 0f;
        float pitch = (p.length >= 6) ? Float.parseFloat(p[5]) : 0f;

        return new Location(world, x, y, z, yaw, pitch);
    }

    public void saveArena(Arena arena) {
        String path = "arenas." + arena.getName();

        plugin.getConfigManager().getArenaConfig().set(path + ".spawn1", locToString(arena.getSpawn1()));
        plugin.getConfigManager().getArenaConfig().set(path + ".spawn2", locToString(arena.getSpawn2()));
        plugin.getConfigManager().getArenaConfig().set(path + ".corner1", locToString(arena.getCorner1()));
        plugin.getConfigManager().getArenaConfig().set(path + ".corner2", locToString(arena.getCorner2()));

        saveArenaSnapshot(arena);
        plugin.getConfigManager().saveArenaConfig();
    }


    private void saveArenaSnapshot(Arena arena) {
        String path = "arenas." + arena.getName() + ".snapshot";

        if (!arena.hasSnapshot()) {
            plugin.getConfigManager().getArenaConfig().set(path, null);
            return;
        }

        plugin.getConfigManager().getArenaConfig().set(path + ".world", arena.getSnapshotWorld());
        plugin.getConfigManager().getArenaConfig().set(path + ".minX", arena.getSnapshotMinX());
        plugin.getConfigManager().getArenaConfig().set(path + ".minY", arena.getSnapshotMinY());
        plugin.getConfigManager().getArenaConfig().set(path + ".minZ", arena.getSnapshotMinZ());
        plugin.getConfigManager().getArenaConfig().set(path + ".maxX", arena.getSnapshotMaxX());
        plugin.getConfigManager().getArenaConfig().set(path + ".maxY", arena.getSnapshotMaxY());
        plugin.getConfigManager().getArenaConfig().set(path + ".maxZ", arena.getSnapshotMaxZ());

        List<String> list = new ArrayList<>();
        for (Map.Entry<BlockVector, BlockData> entry : arena.getOriginalBlocks().entrySet()) {
            BlockVector v = entry.getKey();
            String data = entry.getValue().getAsString();
            list.add(v.getX() + "," + v.getY() + "," + v.getZ() + "|" + data);
        }
        plugin.getConfigManager().getArenaConfig().set(path + ".blocks", list);
    }

    public Arena getAvailableArena() {
        for (Arena arena : availableArenas.values()) {
            if (!arena.isInUse()) {
                return arena;
            }
        }
        return null;
    }

    public Arena getRandomAvailableArena() {
        List<Arena> available = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (!arena.isInUse() && arena.hasSnapshot() &&
                    arena.getSpawn1() != null && arena.getSpawn2() != null &&
                    arena.getCorner1() != null && arena.getCorner2() != null) {
                available.add(arena);
            }

        }

        if (available.isEmpty()) return null;
        return available.get(new Random().nextInt(available.size()));
    }

    public String reserveRandomFreeArenaName() {
        Arena arena = getRandomAvailableArena(); // nutzt deine vorhandene Filter-Logik
        return (arena == null) ? null : arena.getName();
    }

    public void resetArena(Arena arena, Runnable onComplete) {
        if (arena == null || !arena.hasSnapshot()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(arena.getSnapshotWorld());
        if (world == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        // Blocks setzen (sync, weil Block-API main thread)
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Map.Entry<BlockVector, org.bukkit.block.data.BlockData> e : arena.getOriginalBlocks().entrySet()) {
                BlockVector v = e.getKey();
                world.getBlockAt(v.getX(), v.getY(), v.getZ()).setBlockData(e.getValue(), false);
            }
            if (onComplete != null) onComplete.run();
        });
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }

    public Arena getArenaAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;

        for (Arena arena : arenas.values()) {
            if (arena.isInArena(loc)) {
                return arena;
            }
        }
        return null;
    }

    public boolean createArena(String name, Location spawn1, Location spawn2, Location corner1, Location corner2) {
        if (arenas.containsKey(name)) return false;

        Arena arena = new Arena(name);
        arena.setSpawn1(spawn1);
        arena.setSpawn2(spawn2);
        arena.setCorner1(corner1);
        arena.setCorner2(corner2);

        // Snapshot erstellen
        captureArenaSnapshot(arena);

        arenas.put(name, arena);
        availableArenas.put(name, arena);

        saveArena(arena);
        return true;
    }

    private void captureArenaSnapshot(Arena arena) {
        arena.getOriginalBlocks().clear();

        Location c1 = arena.getCorner1();
        Location c2 = arena.getCorner2();
        if (c1 == null || c2 == null || c1.getWorld() == null) return;

        int minX = Math.min(c1.getBlockX(), c2.getBlockX());
        int maxX = Math.max(c1.getBlockX(), c2.getBlockX());
        int minY = Math.min(c1.getBlockY(), c2.getBlockY());
        int maxY = Math.max(c1.getBlockY(), c2.getBlockY());
        int minZ = Math.min(c1.getBlockZ(), c2.getBlockZ());
        int maxZ = Math.max(c1.getBlockZ(), c2.getBlockZ());

        arena.setSnapshotWorld(c1.getWorld().getName());
        arena.setSnapshotBounds(minX, minY, minZ, maxX, maxY, maxZ);

        org.bukkit.World world = c1.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    org.bukkit.block.data.BlockData bd = world.getBlockAt(x, y, z).getBlockData();
                    arena.getOriginalBlocks().put(new BlockVector(x, y, z), bd.clone());
                }
            }
        }
    }

    public boolean deleteArena(String name) {
        Arena arena = arenas.get(name);
        if (arena == null || arena.isInUse()) return false;

        arenas.remove(name);
        availableArenas.remove(name);
        plugin.getConfigManager().getArenaConfig().set("arenas." + name, null);
        plugin.getConfigManager().saveArenaConfig();

        return true;
    }

    public List<String> getArenaNames() {
        return new ArrayList<>(arenas.keySet());
    }

    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
        plugin.getConfigManager().getMainConfig().set("spawn", location);
        plugin.getConfigManager().saveAllConfigs();


    }


    public Location getSpawnLocation() {
        return spawnLocation;
    }

    // Füge diese Methoden zur ArenaManager Klasse hinzu:

    public boolean setArenaSpawn1(String arenaName, Location location) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            arena = new Arena(arenaName);
            arenas.put(arenaName, arena);
            availableArenas.put(arenaName, arena);
        }

        arena.setSpawn1(location);
        saveArena(arena);
        return true;
    }

    public boolean setArenaSpawn2(String arenaName, Location location) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            arena = new Arena(arenaName);
            arenas.put(arenaName, arena);
            availableArenas.put(arenaName, arena);
        }

        arena.setSpawn2(location);
        saveArena(arena);
        return true;
    }

    public boolean setArenaCorner1(String arenaName, Location location) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            arena = new Arena(arenaName);
            arenas.put(arenaName, arena);
            availableArenas.put(arenaName, arena);
        }

        arena.setCorner1(location);
        saveArena(arena);
        return true;
    }

    public boolean setArenaCorner2(String arenaName, Location location) {
        Arena arena = arenas.get(arenaName);
        if (arena == null) {
            arena = new Arena(arenaName);
            arenas.put(arenaName, arena);
            availableArenas.put(arenaName, arena);
        }

        arena.setCorner2(location);
        saveArena(arena);
        return true;
    }

    public boolean createArena(String name) {
        Arena arena = arenas.get(name);
        if (arena == null) {
            return false;
        }

        if (arena.getSpawn1() == null || arena.getSpawn2() == null) {
            return false;
        }

        if (arena.getCorner1() == null || arena.getCorner2() == null) {
            return false;
        }

        // Snapshot erstellen
        captureArenaSnapshot(arena);
        saveArena(arena);
        return true;
    }



    public void cleanup() {
        for (Arena arena : arenas.values()) {
            if (arena.isInUse()) {
                arena.setInUse(false);
            }
        }
        availableArenas.putAll(arenas);
    }
}