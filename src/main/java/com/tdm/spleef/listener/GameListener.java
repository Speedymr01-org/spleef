package com.tdm.spleef.listener;

import com.tdm.spleef.SpleefPlugin;
import com.tdm.spleef.game.GameManager;
import com.tdm.spleef.game.SpleefGame;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

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

        // Cancel all block breaking during game (no natural drops)
        event.setCancelled(true);

        // Any block broken within the arena volume gets removed + gives a snowball
        SpleefGame spleefGame = game.get();
        if (spleefGame.getArena().isWithinBounds(event.getBlock().getLocation())) {
            spleefGame.breakBlock(player, event.getBlock());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Optional<SpleefGame> game = gameManager.getPlayerGame(player);
        if (game.isEmpty()) return;

        SpleefGame spleefGame = game.get();
        SpleefGame.GameState state = spleefGame.getState();

        // Freeze players in lobby — only allow looking around
        if (state == SpleefGame.GameState.WAITING || state == SpleefGame.GameState.COUNTDOWN) {
            Location from = event.getFrom();
            Location to = event.getTo();
            // Cancel if actual position changed (x,y,z), not just rotation (yaw/pitch)
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setCancelled(true);
            }
            return;
        }

        // Check if player fell below the arena floor (into the void)
        if (event.getTo().getY() < spleefGame.getArena().getMinY()) {
            spleefGame.eliminatePlayer(player);
        }

        // Cancel movement if they try to walk outside the arena
        if (state == SpleefGame.GameState.ACTIVE) {
            if (!spleefGame.getArena().isWithinHorizontalBounds(event.getTo())) {
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

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        gameManager.getPlayerGame(player).ifPresent(game -> {
            // If they're still tracked but were eliminated, keep them in spectator at the arena
            if (!game.getAlivePlayers().contains(player) && game.getPlayers().contains(player)) {
                event.setRespawnLocation(game.getArena().getSpawnLocation(0));
                player.setGameMode(GameMode.SPECTATOR);
            }
        });
    }
}
