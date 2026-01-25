package dev.duels.objects;

import java.util.Objects;

public class BlockVector {

    private final int x, y, z;

    public BlockVector(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockVector)) return false;
        BlockVector that = (BlockVector) o;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "BlockVector{x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}