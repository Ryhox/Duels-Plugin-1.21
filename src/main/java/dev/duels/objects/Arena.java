package dev.duels.objects;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Arena {

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private Location corner1;
    private Location corner2;
    private boolean inUse;

    // Snapshot data
    private String snapshotWorld;
    private int snapshotMinX, snapshotMinY, snapshotMinZ;
    private int snapshotMaxX, snapshotMaxY, snapshotMaxZ;
    private final Map<BlockVector, BlockData> originalBlocks = new HashMap<>();
    private final Set<BlockVector> playerPlacedBlocks = new HashSet<>();

    public Arena(String name) {
        this.name = name;
        this.inUse = false;
    }

    public String getName() { return name; }
    public Location getSpawn1() { return spawn1; }
    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public Location getSpawn2() { return spawn2; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }
    public Location getCorner1() { return corner1; }
    public void setCorner1(Location corner1) { this.corner1 = corner1; }
    public Location getCorner2() { return corner2; }
    public void setCorner2(Location corner2) { this.corner2 = corner2; }
    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }

    public boolean hasSnapshot() { return !originalBlocks.isEmpty(); }
    public Map<BlockVector, BlockData> getOriginalBlocks() { return originalBlocks; }
    public String getSnapshotWorld() { return snapshotWorld; }
    public void setSnapshotWorld(String snapshotWorld) { this.snapshotWorld = snapshotWorld; }

    public void setSnapshotBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.snapshotMinX = minX;
        this.snapshotMinY = minY;
        this.snapshotMinZ = minZ;
        this.snapshotMaxX = maxX;
        this.snapshotMaxY = maxY;
        this.snapshotMaxZ = maxZ;
    }

    public int getSnapshotMinX() { return snapshotMinX; }
    public int getSnapshotMinY() { return snapshotMinY; }
    public int getSnapshotMinZ() { return snapshotMinZ; }
    public int getSnapshotMaxX() { return snapshotMaxX; }
    public int getSnapshotMaxY() { return snapshotMaxY; }
    public int getSnapshotMaxZ() { return snapshotMaxZ; }

    public boolean isInArena(Location loc) {
        if (corner1 == null || corner2 == null) return false;
        if (!corner1.getWorld().getUID().equals(loc.getWorld().getUID())) return false;

        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    // Player-placed block tracking
    public void addPlayerPlacedBlock(BlockVector v) { playerPlacedBlocks.add(v); }
    public void removePlayerPlacedBlock(BlockVector v) { playerPlacedBlocks.remove(v); }
    public boolean isPlayerPlacedBlock(BlockVector v) { return playerPlacedBlocks.contains(v); }
    public void clearPlayerPlacedBlocks() { playerPlacedBlocks.clear(); }
}