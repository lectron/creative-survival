package me.sky.creativesurvival.utils;

import org.bukkit.block.Block;

public class Location {
    int x, y, z;
    String world;

    public Location(Block block) {
        this(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public Location(String world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getWorld() {
        return world;
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
            return loc.world.equalsIgnoreCase(world) && loc.x == x && loc.y == y && loc.z == z;
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + world.hashCode();
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;
        return result;
    }
}