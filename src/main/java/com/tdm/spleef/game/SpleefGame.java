package com.tdm.spleef.game;

import com.tdm.spleef.SpleefPlugin;
import com.tdm.spleef.arena.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Represents an active Spleef game.
 */
public class SpleefGame {

    private final SpleefPlugin plugin;
    private final Arena arena;
    private final List<Player> players;
    private final List<Player> alivePlayers;
    private final Set<Location> brokenBlocks;
    private GameState state;
    private int taskId;

    public enum GameState {
        WAITING, COUNTDOWN, ACTIVE, FINISHED
    }

    public SpleefGame(SpleefPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.players = new ArrayList<>();
        this.alivePlayers = new ArrayList<>();
        this.brokenBlocks = new HashSet<>();
        this.state = GameState.WAITING;
        this.taskId = -1;
    }

    public Arena getArena() {
        return arena;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<Player> getAlivePlayers() {
        return Collections.unmodifiableList(alivePlayers);
    }

    public GameState getState() {
        return state;
    }

    public boolean addPlayer(Player player) {
        if (state != GameState.WAITING) {
            player.sendMessage(Component.text("Game is already in progress!", NamedTextColor.RED));
            return false;
        }
        if (players.contains(player)) {
            player.sendMessage(Component.text("You are already in the game!", NamedTextColor.RED));
            return false;
        }
        players.add(player);
        alivePlayers.add(player);
        player.teleport(arena.getSpawnLocation(players.size() - 1));
        player.sendMessage(Component.text("You joined the Spleef game!", NamedTextColor.GREEN));
        broadcast(Component.text(player.getName() + " joined the game! (" + players.size() + " players)", NamedTextColor.YELLOW));
        return true;
    }

    public void removePlayer(Player player) {
        players.remove(player);
        alivePlayers.remove(player);
        player.sendMessage(Component.text("You left the Spleef game.", NamedTextColor.GRAY));
        broadcast(Component.text(player.getName() + " left the game.", NamedTextColor.YELLOW));

        if (players.isEmpty() && state == GameState.WAITING) {
            stop();
        }
    }

    public void start() {
        if (players.size() < 2) {
            broadcast(Component.text("Need at least 2 players to start!", NamedTextColor.RED));
            return;
        }

        state = GameState.COUNTDOWN;
        broadcast(Component.text("Game starting in 5 seconds!", NamedTextColor.GREEN));

        taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (state != GameState.COUNTDOWN) return;
            beginGame();
        }, 100L); // 5 seconds
    }

    private void beginGame() {
        state = GameState.ACTIVE;
        broadcast(Component.text("GO! Break the snow beneath other players!", NamedTextColor.GOLD));

        // Fill the arena floor with snow blocks
        arena.fillFloor();

        for (Player player : players) {
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.DIAMOND_SHOVEL));
            player.setFoodLevel(20);
            player.setSaturation(10);
            player.setHealth(20);
        }
    }

    public void eliminatePlayer(Player player) {
        if (!alivePlayers.contains(player)) return;

        alivePlayers.remove(player);
        player.setGameMode(GameMode.SPECTATOR);
        broadcast(Component.text(player.getName() + " has been eliminated!", NamedTextColor.RED));

        if (alivePlayers.size() <= 1) {
            endGame();
        }
    }

    private void endGame() {
        state = GameState.FINISHED;

        if (alivePlayers.size() == 1) {
            Player winner = alivePlayers.get(0);
            broadcast(Component.text(winner.getName() + " wins the Spleef game!", NamedTextColor.GOLD));
            winner.sendMessage(Component.text("Congratulations! You won!", NamedTextColor.GREEN));
        } else {
            broadcast(Component.text("Game ended! No winners this round.", NamedTextColor.GRAY));
        }

        // Restore broken blocks after a delay
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, this::restoreBlocks, 100L);

        // Kick all players out after 5 seconds
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (Player player : players) {
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                player.getInventory().clear();
            }
            players.clear();
            alivePlayers.clear();
            plugin.getGameManager().removeGame(this);
        }, 100L);
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (state == GameState.ACTIVE || state == GameState.COUNTDOWN) {
            broadcast(Component.text("Game has been stopped.", NamedTextColor.RED));
            for (Player player : players) {
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
                player.getInventory().clear();
            }
        }
        restoreBlocks();
        players.clear();
        alivePlayers.clear();
        state = GameState.FINISHED;
        plugin.getGameManager().removeGame(this);
    }

    public void breakBlock(Player player, Block block) {
        if (state != GameState.ACTIVE) return;
        if (!arena.isWithinBounds(block.getLocation())) return;

        if (block.getType() != Material.AIR) {
            block.setType(Material.AIR);
            block.getState().update(false, false);
            brokenBlocks.add(block.getLocation());

            // Give the player a snowball for every block broken
            player.getInventory().addItem(new ItemStack(Material.SNOWBALL));
        }
    }

    private void restoreBlocks() {
        for (Location loc : brokenBlocks) {
            Block block = loc.getBlock();
            if (block.getType() == Material.AIR) {
                block.setType(Material.SNOW_BLOCK);
                block.getState().update(false, false);
            }
        }
        brokenBlocks.clear();
    }

    private void broadcast(Component message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }
}
