package com.tdm.spleef.game;

import com.tdm.spleef.SpleefPlugin;
import com.tdm.spleef.arena.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
    private final List<GameTeam> teams;
    private final Set<Location> brokenBlocks;
    private final Map<UUID, Location> playerGlass;
    private final Map<UUID, Location> previousLocations;
    private Map<Location, BlockData> arenaSnapshot;
    private GameState state;
    private int taskId;
    private int startedPlayerCount;

    public enum GameState {
        WAITING, COUNTDOWN, ACTIVE, FINISHED
    }

    public SpleefGame(SpleefPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.players = new ArrayList<>();
        this.alivePlayers = new ArrayList<>();
        this.brokenBlocks = new HashSet<>();
        this.playerGlass = new HashMap<>();
        this.previousLocations = new HashMap<>();
        this.arenaSnapshot = null;
        this.teams = new ArrayList<>();
        this.state = GameState.WAITING;
        this.taskId = -1;
        this.startedPlayerCount = 0;
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

    public int getStartedPlayerCount() {
        return startedPlayerCount;
    }

    public List<GameTeam> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    public boolean isTeamGame() {
        return !teams.isEmpty();
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

        int maxPlayers = arena.getMinPlayers();
        if (players.size() >= maxPlayers) {
            player.sendMessage(Component.text("Game is full (" + maxPlayers + "/" + maxPlayers + ")!", NamedTextColor.RED));
            return false;
        }

        boolean wasEmpty = players.isEmpty();

        // Save the player's current location so we can return them there on leave/game end
        previousLocations.put(player.getUniqueId(), player.getLocation().clone());

        players.add(player);
        alivePlayers.add(player);
        Location spawn = arena.getSpawnLocation(players.size() - 1);
        player.teleport(spawn);
        player.sendMessage(Component.text("You joined the Spleef game!", NamedTextColor.GREEN));
        broadcast(Component.text(player.getName() + " joined the game! (" + players.size() + "/" + maxPlayers + ")", NamedTextColor.YELLOW));

        // Fill the arena floor with snow when the first player joins
        if (wasEmpty) {
            arena.fillFloor();
        }

        // Place a glass block directly below the spawn point
        // Only if there's a drop (the block below spawn is air)
        Location glassLoc = spawn.clone().subtract(0, 1, 0);
        Block below = glassLoc.getBlock();
        if (below.getType() == Material.AIR) {
            below.setType(Material.GLASS);
            below.getState().update(false, false);
            playerGlass.put(player.getUniqueId(), glassLoc.clone());
        }

        return true;
    }

    public void removePlayer(Player player) {
        players.remove(player);
        alivePlayers.remove(player);
        if (isTeamGame()) {
            GameTeam team = GameTeam.getPlayerTeam(teams, player);
            if (team != null) team.removeMember(player);
        }

        // Remove the player's glass block if present
        removePlayerGlass(player);

        // Teleport the player back to where they were before joining
        teleportBack(player);

        player.sendMessage(Component.text("You left the Spleef game.", NamedTextColor.GRAY));
        broadcast(Component.text(player.getName() + " left the game.", NamedTextColor.YELLOW));

        if (players.isEmpty() && (state == GameState.WAITING || state == GameState.COUNTDOWN)) {
            stop();
        }
    }

    private void removePlayerGlass(Player player) {
        Location glassLoc = playerGlass.remove(player.getUniqueId());
        if (glassLoc != null) {
            Block block = glassLoc.getBlock();
            if (block.getType() == Material.GLASS) {
                block.setType(Material.AIR);
                block.getState().update(false, false);
            }
        }
    }

    private void removeAllGlassBlocks() {
        for (Location loc : playerGlass.values()) {
            Block block = loc.getBlock();
            if (block.getType() == Material.GLASS) {
                block.setType(Material.AIR);
                block.getState().update(false, false);
            }
        }
        playerGlass.clear();
    }

    private void snapshotArena() {
        World world = arena.getWorld();
        arenaSnapshot = new HashMap<>();
        for (int x = arena.getMinX(); x <= arena.getMaxX(); x++) {
            for (int y = arena.getMinY(); y <= arena.getMaxY(); y++) {
                for (int z = arena.getMinZ(); z <= arena.getMaxZ(); z++) {
                    Location loc = new Location(world, x, y, z);
                    arenaSnapshot.put(loc, loc.getBlock().getBlockData());
                }
            }
        }
    }

    private void restoreArenaSnapshot() {
        if (arenaSnapshot == null) return;
        for (Map.Entry<Location, BlockData> entry : arenaSnapshot.entrySet()) {
            Block block = entry.getKey().getBlock();
            block.setBlockData(entry.getValue(), false);
        }
        arenaSnapshot = null;
    }

    private void teleportBack(Player player) {
        Location prev = previousLocations.remove(player.getUniqueId());
        if (prev != null) {
            player.teleport(prev);
        } else {
            player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
        }
    }

    private void teleportAllBack() {
        for (Player player : players) {
            teleportBack(player);
        }
        previousLocations.clear();
    }

    public void start() {
        if (state != GameState.WAITING) {
            broadcast(Component.text("Game cannot be started in its current state!", NamedTextColor.RED));
            return;
        }
        if (players.isEmpty()) {
            broadcast(Component.text("No players in the game!", NamedTextColor.RED));
            return;
        }
        int needed = arena.getMinPlayers();
        if (players.size() < needed) {
            broadcast(Component.text("Need at least " + needed + " players to start (have " + players.size() + ")!", NamedTextColor.RED));
            return;
        }

        // For team modes, player count must be a multiple of team size
        int teamSize = getTeamSizeForType();
        if (teamSize > 1 && players.size() % teamSize != 0) {
            broadcast(Component.text("Player count (" + players.size() + ") must be a multiple of " + teamSize + " for " + arena.getGameType() + " mode!", NamedTextColor.RED));
            return;
        }

        // Require enough spawn points — need at least `needed` spawns for all players
        if (!arena.getSpawnLocations().isEmpty() && arena.getSpawnLocations().size() < needed) {
            broadcast(Component.text("Arena needs at least " + needed + " spawn points (has " + arena.getSpawnLocations().size() + ")!", NamedTextColor.RED));
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
        startedPlayerCount = players.size();

        // Create teams based on arena type
        teams.clear();
        int teamSize = getTeamSizeForType();
        if (teamSize > 1) {
            teams.addAll(GameTeam.createTeams(players, teamSize));
            broadcast(Component.text("Teams:", NamedTextColor.GOLD));
            for (GameTeam team : teams) {
                StringBuilder memberList = new StringBuilder();
                for (Player member : team.getMembers()) {
                    if (memberList.length() > 0) memberList.append(", ");
                    memberList.append(member.getName());
                }
                broadcast(team.getDisplayName().append(Component.text(": " + memberList.toString(), NamedTextColor.WHITE)));
            }
        }

        // Remove glass platforms so players drop onto the snow
        removeAllGlassBlocks();

        // Fill the arena floor with snow blocks
        arena.fillFloor();

        // Snapshot the entire arena volume so we can fully reset it later
        snapshotArena();

        broadcast(Component.text("GO! Break the snow beneath other players!", NamedTextColor.GOLD));

        for (Player player : players) {
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.getInventory().addItem(new ItemStack(Material.DIAMOND_SHOVEL));
            player.setFoodLevel(20);
            player.setSaturation(10);
            player.setHealth(20);
        }
    }

    /**
     * Checks if two players are on the same team in a team game.
     */
    public boolean arePlayersSameTeam(Player a, Player b) {
        if (!isTeamGame()) return false;
        GameTeam teamA = GameTeam.getPlayerTeam(teams, a);
        GameTeam teamB = GameTeam.getPlayerTeam(teams, b);
        return teamA != null && teamA == teamB;
    }

    private int getTeamSizeForType() {
        switch (arena.getGameType()) {
            case "duos": return 2;
            case "trios": return 3;
            case "quads": return 4;
            default: return 0; // ffa/solos = no teams
        }
    }

    public void eliminatePlayer(Player player) {
        if (!alivePlayers.contains(player)) return;

        alivePlayers.remove(player);
        player.setGameMode(GameMode.SPECTATOR);

        if (isTeamGame()) {
            GameTeam team = GameTeam.getPlayerTeam(teams, player);
            if (team != null) {
                boolean teamDead = team.eliminateMember(player);
                broadcast(team.getPlayerDisplayName(player).append(Component.text(" has been eliminated!", NamedTextColor.RED)));
                if (teamDead) {
                    broadcast(team.getDisplayName().append(Component.text(" has been eliminated!", NamedTextColor.RED)));
                }
            }
        } else {
            broadcast(Component.text(player.getName() + " has been eliminated!", NamedTextColor.RED));
        }

        int aliveTeamCount = isTeamGame() ? (int) teams.stream().filter(GameTeam::isAlive).count() : alivePlayers.size();
        if (aliveTeamCount <= 1) {
            endGame();
        }
    }

    private void endGame() {
        state = GameState.FINISHED;

        if (isTeamGame()) {
            List<GameTeam> aliveTeams = teams.stream().filter(GameTeam::isAlive).toList();
            if (aliveTeams.size() == 1) {
                GameTeam winner = aliveTeams.get(0);
                broadcast(winner.getDisplayName().append(Component.text(" wins the Spleef game!", NamedTextColor.GOLD)));
                for (Player member : winner.getMembers()) {
                    member.sendMessage(Component.text("Congratulations! Your team won!", NamedTextColor.GREEN));
                }
            } else {
                broadcast(Component.text("The game ended in a tie!", NamedTextColor.GRAY));
            }
        } else if (alivePlayers.size() == 1) {
            Player winner = alivePlayers.get(0);
            broadcast(Component.text(winner.getName() + " wins the Spleef game!", NamedTextColor.GOLD));
            winner.sendMessage(Component.text("Congratulations! You won!", NamedTextColor.GREEN));
        } else if (alivePlayers.isEmpty()) {
            broadcast(Component.text("The game ended in a tie!", NamedTextColor.GRAY));
        } else {
            broadcast(Component.text("Game ended! No winners this round.", NamedTextColor.GRAY));
        }

        // Restore the arena to pre-game state and kick players out
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (Player player : players) {
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().clear();
            }
            teleportAllBack();
            restoreArenaSnapshot();
            players.clear();
            alivePlayers.clear();
            teams.clear();
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
                player.getInventory().clear();
            }
        }
        teleportAllBack();
        restoreArenaSnapshot();
        players.clear();
        alivePlayers.clear();
        teams.clear();
        state = GameState.FINISHED;
        plugin.getGameManager().removeGame(this);
    }

    public void breakBlock(Player player, Block block) {
        if (state != GameState.ACTIVE) return;
        if (!arena.isWithinBounds(block.getLocation())) return;

        // In team games, prevent breaking blocks directly below a teammate
        if (isTeamGame()) {
            int bx = block.getX(), by = block.getY(), bz = block.getZ();
            for (Player teammate : players) {
                if (teammate == player) continue;
                if (!arePlayersSameTeam(player, teammate)) continue;
                Location loc = teammate.getLocation();
                if (loc.getBlockX() == bx && loc.getBlockY() == by + 1 && loc.getBlockZ() == bz) {
                    return; // Block is directly below a teammate
                }
            }
        }

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
