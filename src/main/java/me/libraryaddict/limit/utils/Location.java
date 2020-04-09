package me.libraryaddict.limit.utils;

import org.bukkit.block.Block;

public class Location {
    int x, y, z;

    public Location(Block block) {
        this(block.getX(), block.getY(), block.getZ());
    }

    public Location(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Location) {
            Location loc = (Location) obj;
            return loc.x == x && loc.y == y && loc.z == z;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;
        return result;
    }
}