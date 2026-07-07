package com.tdm.spleef.listener;

import com.tdm.spleef.SpleefPlugin;
import com.tdm.spleef.game.GameManager;
import com.tdm.spleef.game.SpleefGame;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class GameListener implements Listener {

    private final SpleefPlugin plugin;
    private final GameManager gameManager;

    public GameListener(SpleefPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Optional<SpleefGame> game = gameManager.getPlayerGame(player);
        if (game.isEmpty()) return;

        event.setCancelled(true);

        // Only allow breaking snow blocks in the arena floor
        if (event.getBlock().getType() == Material.SNOW_BLOCK) {
            SpleefGame spleefGame = game.get();
            if (spleefGame.getArena().isWithinFloor(event.getBlock().getLocation())) {
                event.setCancelled(false);
                spleefGame.breakBlock(event.getBlock());
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Optional<SpleefGame> game = gameManager.getPlayerGame(player);
        if (game.isEmpty()) return;

        // Check if player fell below the arena floor (into the void)
        SpleefGame spleefGame = game.get();
        if (event.getTo().getY() < spleefGame.getArena().getMinY()) {
            spleefGame.eliminatePlayer(player);
        }

        // Cancel movement if they try to walk outside the arena
        if (spleefGame.getState() == SpleefGame.GameState.ACTIVE) {
            if (!spleefGame.getArena().isWithinFloor(event.getTo())) {
                // Don't cancel if they're already eliminated (spectating)
                if (spleefGame.getAlivePlayers().contains(player)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        Optional<SpleefGame> game = gameManager.getPlayerGame(player);
        if (game.isPresent()) {
            // Only void damage should eliminate players
            if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        gameManager.getPlayerGame(player).ifPresent(game -> game.removePlayer(player));
    }
}
