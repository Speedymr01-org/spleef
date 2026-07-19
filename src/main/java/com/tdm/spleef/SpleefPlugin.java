package com.tdm.spleef;

import com.tdm.spleef.api.SpleefAPI;
import com.tdm.spleef.command.SpleefCommand;
import com.tdm.spleef.game.GameManager;
import com.tdm.spleef.listener.GameListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class SpleefPlugin extends JavaPlugin {

    private GameManager gameManager;
    private SpleefMinigameProvider provider;

    @Override
    public void onEnable() {
        saveDefaultConfig();

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
}
