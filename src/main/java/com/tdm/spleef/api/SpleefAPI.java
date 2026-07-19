package com.tdm.spleef.api;

import com.tdm.spleef.SpleefPlugin;
import com.tdm.spleef.arena.Arena;
import com.tdm.spleef.game.GameManager;
import com.tdm.spleef.game.SpleefGame;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Unofficial API for the Spleef plugin.
 * <p>
 * Other plugins (e.g. a tournament plugin) can obtain this API via Bukkit's ServiceManager:
 * <pre>{@code
 * RegisteredServiceProvider<SpleefAPI> provider = Bukkit.getServicesManager().getRegistration(SpleefAPI.class);
 * if (provider != null) {
 *     SpleefAPI spleefAPI = provider.getProvider();
 * }
 * }</pre>
 */
public class SpleefAPI {

    private final SpleefPlugin plugin;
    private final GameManager gameManager;

    public SpleefAPI(SpleefPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /** Returns the plugin instance. */
    public SpleefPlugin getPlugin() {
        return plugin;
    }

    // ──────────────────────────────────────────────
    //  Arena queries
    // ──────────────────────────────────────────────

    /** Returns a fully created arena by name, if it exists. */
    public Optional<Arena> getArena(String name) {
        return gameManager.getArena(name);
    }

    /** Returns all fully created arenas. */
    public Collection<Arena> getArenas() {
        return gameManager.getArenas();
    }

    /** Returns the names of pending (not yet fully configured) arenas. */
    public Set<String> getPendingArenas() {
        return gameManager.getPendingArenas();
    }

    /** Returns true if the given arena name is registered (pending or fully created). */
    public boolean isArenaRegistered(String name) {
        return gameManager.isArenaRegistered(name);
    }

    // ──────────────────────────────────────────────
    //  Player queries
    // ──────────────────────────────────────────────

    /** Returns true if the player is currently in any active game. */
    public boolean isPlayerInGame(Player player) {
        return gameManager.isPlayerInGame(player);
    }

    /** Returns the active game the player is in, if any. */
    public Optional<SpleefGame> getPlayerGame(Player player) {
        return gameManager.getPlayerGame(player);
    }

    // ──────────────────────────────────────────────
    //  Game queries
    // ──────────────────────────────────────────────

    /** Returns the active game for the given arena name, if it exists. */
    public Optional<SpleefGame> getGame(String arenaName) {
        return gameManager.getGame(arenaName);
    }

    /** Returns all currently active games. */
    public Collection<SpleefGame> getActiveGames() {
        return gameManager.getActiveGames();
    }

    /**
     * Returns all active games that match the given state.
     * @param state the game state to filter by
     * @return games in the specified state
     */
    public List<SpleefGame> getActiveGamesByState(SpleefGame.GameState state) {
        return gameManager.getActiveGames().stream()
                .filter(g -> g.getState() == state)
                .toList();
    }

    // ──────────────────────────────────────────────
    //  Game management
    // ──────────────────────────────────────────────

    /**
     * Creates a new game on the given arena and registers it.
     * @param arena the arena to create a game on
     * @return the newly created SpleefGame
     */
    public SpleefGame createGame(Arena arena) {
        return gameManager.createGame(arena);
    }

    /**
     * Removes a game from the active games registry.
     * @param game the game to remove
     */
    public void removeGame(SpleefGame game) {
        gameManager.removeGame(game);
    }

    /**
     * Stops all active games.
     */
    public void stopAllGames() {
        gameManager.stopAllGames();
    }
}
