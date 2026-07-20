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
        // Find the arena
        var arenaOpt = api.getArena(arena);
        if (arenaOpt.isEmpty()) return false;

        Arena a = arenaOpt.get();

        // Create the game on the arena
        SpleefGame game = api.createGame(a);
        if (game == null) return false;

        // Add all players from both teams
        List<Player> allPlayers = new ArrayList<>();
        for (UUID uid : team1) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                game.addPlayer(p);
                allPlayers.add(p);
            }
        }
        for (UUID uid : team2) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                game.addPlayer(p);
                allPlayers.add(p);
            }
        }

        // Store match context for later result mapping
        activeMatches.put(matchId, new MatchContext(arena, team1, team2, allPlayers));

        // Start the game
        game.start();
        return true;
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

        if (matchId == null || ctx == null) return;
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
