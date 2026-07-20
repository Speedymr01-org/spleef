package com.Speedymr01.spleef.api.event;

import com.Speedymr01.spleef.game.SpleefGame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player is eliminated from a Spleef game.
 */
public class SpleefPlayerEliminateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SpleefGame game;
    private final boolean teamEliminated;

    /**
     * @param player          the eliminated player
     * @param game            the game
     * @param teamEliminated  true if this elimination caused the player's entire team to be eliminated
     */
    public SpleefPlayerEliminateEvent(Player player, SpleefGame game, boolean teamEliminated) {
        this.player = player;
        this.game = game;
        this.teamEliminated = teamEliminated;
    }

    /** The eliminated player. */
    public Player getPlayer() {
        return player;
    }

    /** The game the player was eliminated from. */
    public SpleefGame getGame() {
        return game;
    }

    /**
     * Returns true if this elimination caused the entire team to be eliminated.
     * Always false in FFA / solos mode.
     */
    public boolean isTeamEliminated() {
        return teamEliminated;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
