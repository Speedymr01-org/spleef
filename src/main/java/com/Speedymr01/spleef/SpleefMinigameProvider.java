package com.Speedymr01.spleef;

import com.Speedymr01.spleef.api.SpleefAPI;
import com.Speedymr01.spleef.api.event.SpleefGameEndEvent;
import com.Speedymr01.spleef.arena.Arena;
import com.Speedymr01.spleef.game.SpleefGame;
import com.tdm.tournament.api.MatchCompleteEvent;
import com.tdm.tournament.api.MinigameProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridges Spleef with the TournamentManager plugin via {@link MinigameProvider}.
 * Registered in Bukkit's ServicesManager when Spleef enables.
 */
public class SpleefMinigameProvider implements MinigameProvider, Listener {

    private final SpleefPlugin plugin;
    private final SpleefAPI api;

    // Maps tournament match IDs to arena + team info so we can fire MatchCompleteEvent
    private final Map<String, MatchContext> activeMatches = new HashMap<>();

    // When true, game end event should be ignored (cleanup from failed createMatch)
    private volatile boolean endingForCleanup = false;

    public SpleefMinigameProvider(SpleefPlugin plugin, SpleefAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    public void register() {
        Bukkit.getServicesManager().register(MinigameProvider.class, this, plugin, ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Registered MinigameProvider for TournamentManager");
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        Bukkit.getServicesManager().unregister(MinigameProvider.class, this);
        activeMatches.clear();
    }

    // ==================== MinigameProvider ====================

    @Override
    public String getPluginName() {
        return "Spleef";
    }

    @Override
    public String getDisplayName() {
        return "Spleef";
    }

    @Override
    public Material getIcon() {
        return Material.SNOWBALL;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public List<String> getAvailableArenas() {
        return api.getArenas().stream()
                .map(Arena::getName)
                .collect(Collectors.toList());
    }

    @Override
    public boolean createMatch(String arena, List<UUID> team1, List<UUID> team2, String matchId) {
        plugin.verbose("createMatch called: arena=" + arena + " matchId=" + matchId
                + " team1=" + team1.size() + " players, team2=" + team2.size() + " players");

        // Step 1: Find the arena
        var arenaOpt = api.getArena(arena);
        if (arenaOpt.isEmpty()) {
            plugin.verbose("createMatch: FAILED — arena '" + arena + "' not found");
            return false;
        }
        Arena a = arenaOpt.get();

        // Step 2: Create the game on the arena
        plugin.verbose("createMatch: creating game on arena '" + arena + "'");
        SpleefGame game = api.createGame(a);
        if (game == null) {
            plugin.verbose("createMatch: FAILED — api.createGame returned null");
            return false;
        }

        boolean needCleanup = true;

        try {
            // Step 3: Add all players from both teams
            List<Player> allPlayers = new ArrayList<>();
            for (UUID uid : team1) {
                Player p = Bukkit.getPlayer(uid);
                boolean found = (p != null && p.isOnline());
                plugin.verbose("createMatch: team1 player uid=" + uid + " found=" + found + " name=" + (p != null ? p.getName() : "N/A"));
                if (found) {
                    game.addPlayer(p);
                    allPlayers.add(p);
                }
            }
            for (UUID uid : team2) {
                Player p = Bukkit.getPlayer(uid);
                boolean found = (p != null && p.isOnline());
                plugin.verbose("createMatch: team2 player uid=" + uid + " found=" + found + " name=" + (p != null ? p.getName() : "N/A"));
                if (found) {
                    game.addPlayer(p);
                    allPlayers.add(p);
                }
            }

            plugin.verbose("createMatch: " + allPlayers.size() + " total players joined");

            if (allPlayers.isEmpty()) {
                plugin.verbose("createMatch: FAILED — no players joined");
                return false;
            }

            // Step 4: Store match context for later result mapping
            activeMatches.put(matchId, new MatchContext(arena, team1, team2, allPlayers));

            // Step 5: Start the game
            plugin.verbose("createMatch: starting game...");
            game.start();
            plugin.verbose("createMatch: SUCCESS — match " + matchId + " started on arena '" + arena + "'");
            needCleanup = false;
            return true;
        } finally {
            // If we created the game but something failed, stop it so next attempt can work
            if (needCleanup) {
                plugin.verbose("createMatch: cleaning up — stopping spleef game after failed match start");
                endingForCleanup = true;
                try {
                    api.getGame(arena).ifPresent(SpleefGame::stop);
                    api.removeGame(game);
                } finally {
                    endingForCleanup = false;
                }
                activeMatches.remove(matchId);
            }
        }
    }

    @Override
    public void cancelMatch(String matchId) {
        MatchContext ctx = activeMatches.remove(matchId);
        if (ctx == null) return;

        // Find and stop the game on this arena
        api.getGame(ctx.arena).ifPresent(SpleefGame::stop);
    }

    // ==================== Listen for Spleef game end ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpleefGameEnd(SpleefGameEndEvent event) {
        // Ignore game-end events triggered by cleanup of a failed createMatch
        if (endingForCleanup) return;

        String arenaName = event.getGame().getArena().getName();

        // Find which of our tracked matches matches this arena
        String matchId = null;
        MatchContext ctx = null;
        for (Map.Entry<String, MatchContext> entry : activeMatches.entrySet()) {
            if (entry.getValue().arena.equals(arenaName)) {
                matchId = entry.getKey();
                ctx = entry.getValue();
                break;
            }
        }

        if (matchId == null || ctx == null) {
            plugin.verbose("onSpleefGameEnd: no active match for arena '" + arenaName + "', ignoring");
            return;
        }
        activeMatches.remove(matchId);

        // Determine winners
        List<Player> winnerPlayers = event.getWinners();
        boolean tie = winnerPlayers == null || winnerPlayers.isEmpty();
        List<UUID> winnerUuids = tie ? List.of()
                : winnerPlayers.stream().map(Player::getUniqueId).collect(Collectors.toList());

        // Fire MatchCompleteEvent
        MatchCompleteEvent completeEvent = new MatchCompleteEvent(
                getPluginName(), matchId, winnerUuids, arenaName, tie);
        Bukkit.getPluginManager().callEvent(completeEvent);
    }

    // ==================== Context ====================

    private static class MatchContext {
        final String arena;
        final List<UUID> team1;
        final List<UUID> team2;
        final List<Player> allPlayers;

        MatchContext(String arena, List<UUID> team1, List<UUID> team2, List<Player> allPlayers) {
            this.arena = arena;
            this.team1 = team1;
            this.team2 = team2;
            this.allPlayers = allPlayers;
        }
    }
}
