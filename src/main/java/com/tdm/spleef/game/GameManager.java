package com.tdm.spleef.game;

import com.tdm.spleef.SpleefPlugin;
import com.tdm.spleef.arena.Arena;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages Spleef games and arenas.
 */
public class GameManager {

    private final SpleefPlugin plugin;
    private final Map<String, Arena> arenas;
    private final Map<String, SpleefGame> activeGames;
    private final Set<String> pendingArenas;

    public GameManager(SpleefPlugin plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.activeGames = new HashMap<>();
        this.pendingArenas = new HashSet<>();
        loadPendingArenas();
        loadArenas();
    }

    private void loadPendingArenas() {
        List<String> pending = plugin.getConfig().getStringList("pending-arenas");
        pendingArenas.addAll(pending);
    }

    private void savePendingArenas() {
        plugin.getConfig().set("pending-arenas", new ArrayList<>(pendingArenas));
        plugin.saveConfig();
    }

    /**
     * Creates a pending (unbounded) arena. Bounds must be set later with setarena.
     */
    public boolean createPendingArena(String name) {
        if (arenas.containsKey(name) || pendingArenas.contains(name)) return false;
        pendingArenas.add(name);
        savePendingArenas();
        return true;
    }

    /**
     * Checks if a name is registered as an arena (fully created or pending).
     */
    public boolean isArenaRegistered(String name) {
        return arenas.containsKey(name) || pendingArenas.contains(name);
    }

    /**
     * Removes the pending flag after a full arena is created.
     */
    public void removePending(String name) {
        pendingArenas.remove(name);
        savePendingArenas();
    }

    private void loadArenas() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");
        if (arenasSection != null) {
            for (String key : arenasSection.getKeys(false)) {
                ConfigurationSection section = arenasSection.getConfigurationSection(key);
                if (section != null) {
                    try {
                        Arena arena = Arena.load(section);
                        arenas.put(arena.getName(), arena);
                        plugin.getLogger().info("Loaded arena: " + arena.getName());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load arena '" + key + "': " + e.getMessage());
                    }
                }
            }
        }
    }

    public void saveArena(Arena arena) {
        arenas.put(arena.getName(), arena);
        ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("arenas");
        if (arenasSection == null) {
            arenasSection = plugin.getConfig().createSection("arenas");
        }
        arena.save(arenasSection.createSection(arena.getName()));
        plugin.saveConfig();
    }

    public Optional<Arena> getArena(String name) {
        return Optional.ofNullable(arenas.get(name));
    }

    public Collection<Arena> getArenas() {
        return arenas.values();
    }

    public boolean hasArena(String name) {
        return arenas.containsKey(name);
    }

    public Optional<SpleefGame> getGame(String name) {
        return Optional.ofNullable(activeGames.get(name));
    }

    public Collection<SpleefGame> getActiveGames() {
        return activeGames.values();
    }

    public boolean isPlayerInGame(Player player) {
        return activeGames.values().stream().anyMatch(g -> g.getPlayers().contains(player));
    }

    public Optional<SpleefGame> getPlayerGame(Player player) {
        return activeGames.values().stream()
                .filter(g -> g.getPlayers().contains(player))
                .findFirst();
    }

    public SpleefGame createGame(Arena arena) {
        SpleefGame game = new SpleefGame(plugin, arena);
        activeGames.put(arena.getName(), game);
        return game;
    }

    public void removeGame(SpleefGame game) {
        activeGames.values().remove(game);
    }

    public void stopAllGames() {
        new HashMap<>(activeGames).forEach((name, game) -> game.stop());
    }
}
