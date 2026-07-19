package com.tdm.spleef.api.event;

import com.tdm.spleef.game.SpleefGame;
import com.tdm.spleef.game.SpleefGame.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Called when a Spleef game ends.
 * Contains the game reference, winner information, and final state.
 */
public class SpleefGameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SpleefGame game;
    private final List<Player> winners;
    private final GameState finalState;

    /**
     * @param game       the game that ended
     * @param winners    the winning player(s) – a single player for FFA, all members of the winning team for team modes
     * @param finalState the final game state
     */
    public SpleefGameEndEvent(SpleefGame game, List<Player> winners, GameState finalState) {
        this.game = game;
        this.winners = winners;
        this.finalState = finalState;
    }

    /** The game that ended. */
    public SpleefGame getGame() {
        return game;
    }

    /**
     * The winning player(s):
     * <ul>
     *   <li>FFA / solos: a single player</li>
     *   <li>Team modes: all members of the winning team</li>
     * </ul>
     * Empty list if the game ended in a tie.
     */
    public List<Player> getWinners() {
        return Collections.unmodifiableList(winners);
    }

    /** The final state of the game (FINISHED). */
    public GameState getFinalState() {
        return finalState;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
