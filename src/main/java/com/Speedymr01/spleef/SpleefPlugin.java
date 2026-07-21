package com.Speedymr01.spleef;

import com.Speedymr01.spleef.api.SpleefAPI;
import com.Speedymr01.spleef.command.SpleefCommand;
import com.Speedymr01.spleef.game.GameManager;
import com.Speedymr01.spleef.listener.GameListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

public class SpleefPlugin extends JavaPlugin {

    private GameManager gameManager;
    private SpleefMinigameProvider provider;

    // GUI handlers for config menu
    private final Map<UUID, BiFunction<Player, Integer, Boolean>> guiHandlers = new HashMap<>();

    private final Listener guiListener = new Listener() {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            BiFunction<Player, Integer, Boolean> handler = guiHandlers.get(player.getUniqueId());
            if (handler != null) {
                boolean handled = handler.apply(player, event.getSlot());
                if (handled) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player)) return;
            Player player = (Player) event.getPlayer();
            guiHandlers.remove(player.getUniqueId());
        }
    };

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Register GUI click listener for config menus
        getServer().getPluginManager().registerEvents(guiListener, this);

        // bStats
        int pluginId = 32518;
        Metrics metrics = new Metrics(this, pluginId);

        this.gameManager = new GameManager(this);

        getCommand("spleef").setExecutor(new SpleefCommand(this, gameManager));
        getServer().getPluginManager().registerEvents(new GameListener(this, gameManager), this);

        // Register unofficial API for other plugins (e.g. tournament)
        SpleefAPI spleefAPI = new SpleefAPI(this, gameManager);
        getServer().getServicesManager().register(SpleefAPI.class, spleefAPI, this, org.bukkit.plugin.ServicePriority.Normal);
        getLogger().info("Registered SpleefAPI for external plugins");

        // Register TournamentManager MinigameProvider (if TournamentManager is installed)
        if (isTournamentManagerInstalled()) {
            this.provider = new SpleefMinigameProvider(this, spleefAPI);
            this.provider.register();
        }

        getLogger().info("Spleef v" + getDescription().getVersion() + " enabled!");
    }

    private static SpleefPlugin instance;

    public static SpleefPlugin getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        if (provider != null) {
            provider.unregister();
        }
        if (gameManager != null) {
            gameManager.stopAllGames();
        }
        getLogger().info("Spleef disabled!");
    }

    private boolean isTournamentManagerInstalled() {
        try {
            Class.forName("com.tdm.tournament.api.MinigameProvider");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Log a verbose diagnostic message (prefixed with [VERBOSE]).
     * Controlled by {@code verbose-logging} in config.yml.
     */
    public void verbose(String message) {
        if (getConfig().getBoolean("verbose-logging", true)) {
            getLogger().info("[VERBOSE] " + message);
        }
    }

    /**
     * Reload all cached config values from config.yml.
     * Ready for future config options.
     */
    public void loadConfigSettings() {
        // Future config values will be loaded here
    }

    /**
     * Set a handler for inventory clicks.
     */
    public void setGuiHandler(UUID playerId, BiFunction<Player, Integer, Boolean> handler) {
        guiHandlers.put(playerId, handler);
    }
}
