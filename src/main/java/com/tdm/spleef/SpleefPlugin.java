package com.tdm.spleef;

import com.tdm.spleef.command.SpleefCommand;
import com.tdm.spleef.game.GameManager;
import com.tdm.spleef.listener.GameListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class SpleefPlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // bStats
        int pluginId = 32518;
        Metrics metrics = new Metrics(this, pluginId);

        this.gameManager = new GameManager(this);

        getCommand("spleef").setExecutor(new SpleefCommand(this, gameManager));
        getServer().getPluginManager().registerEvents(new GameListener(this, gameManager), this);

        getLogger().info("Spleef v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.stopAllGames();
        }
        getLogger().info("Spleef disabled!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
