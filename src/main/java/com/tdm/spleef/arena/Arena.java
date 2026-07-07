package com.tdm.spleef.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Spleef arena with a floor area and spawn points.
 */
public class Arena {

    private final String name;
    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final List<Location> spawnLocations;

    public Arena(String name, World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.name = name;
        this.world = world;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.min(minY + 1, Math.max(minY, maxY)); // floor is at minY
        this.maxZ = Math.max(minZ, maxZ);
        this.spawnLocations = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    /**
     * Checks if a location is within the arena floor bounds.
     */
    public boolean isWithinFloor(Location location) {
        if (!location.getWorld().equals(world)) return false;
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * Checks if a location is on the floor level.
     */
    public boolean isOnFloor(Location location) {
        return isWithinFloor(location) && location.getBlockY() == minY;
    }

    public Location getSpawnLocation(int index) {
        if (spawnLocations.isEmpty()) {
            // Default spawn: center of arena
            int centerX = (minX + maxX) / 2;
            int centerZ = (minZ + maxZ) / 2;
            return new Location(world, centerX + 0.5, minY + 1, centerZ + 0.5);
        }
        return spawnLocations.get(index % spawnLocations.size());
    }

    public void addSpawnLocation(Location location) {
        spawnLocations.add(location.clone());
    }

    public List<Location> getSpawnLocations() {
        return new ArrayList<>(spawnLocations);
    }

    /**
     * Saves the arena configuration to the given config section.
     */
    public void save(ConfigurationSection section) {
        section.set("world", world.getName());
        section.set("minX", minX);
        section.set("minY", minY);
        section.set("minZ", minZ);
        section.set("maxX", maxX);
        section.set("maxY", maxY);
        section.set("maxZ", maxZ);

        ConfigurationSection spawnsSection = section.createSection("spawns");
        int i = 0;
        for (Location spawn : spawnLocations) {
            ConfigurationSection s = spawnsSection.createSection(String.valueOf(i++));
            s.set("x", spawn.getX());
            s.set("y", spawn.getY());
            s.set("z", spawn.getZ());
            s.set("yaw", (double) spawn.getYaw());
            s.set("pitch", (double) spawn.getPitch());
        }
    }

    /**
     * Loads an arena from a configuration section.
     */
    public static Arena load(ConfigurationSection section) {
        String worldName = section.getString("world");
        World world = java.util.Objects.requireNonNull(
                org.bukkit.Bukkit.getWorld(worldName),
                "World '" + worldName + "' not found!"
        );
        int minX = section.getInt("minX");
        int minY = section.getInt("minY");
        int minZ = section.getInt("minZ");
        int maxX = section.getInt("maxX");
        int maxY = section.getInt("maxY");
        int maxZ = section.getInt("maxZ");

        Arena arena = new Arena(section.getName(), world, minX, minY, minZ, maxX, maxY, maxZ);

        ConfigurationSection spawnsSection = section.getConfigurationSection("spawns");
        if (spawnsSection != null) {
            for (String key : spawnsSection.getKeys(false)) {
                ConfigurationSection s = spawnsSection.getConfigurationSection(key);
                if (s != null) {
                    Location loc = new Location(
                            world,
                            s.getDouble("x"),
                            s.getDouble("y"),
                            s.getDouble("z"),
                            (float) s.getDouble("yaw", 0.0),
                            (float) s.getDouble("pitch", 0.0)
                    );
                    arena.addSpawnLocation(loc);
                }
            }
        }

        return arena;
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
}
