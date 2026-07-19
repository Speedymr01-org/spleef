package com.tdm.spleef.api.event;

import com.tdm.spleef.game.SpleefGame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player leaves a Spleef game (via command, quit, or game end).
 */
public class SpleefPlayerLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SpleefGame game;
    private final LeaveReason reason;

    public enum LeaveReason {
        /** Player used /spleef leave or quit the server. */
        LEAVE,
        /** Player was eliminated during the game. */
        ELIMINATED,
        /** Game ended. */
        GAME_END
    }

    public SpleefPlayerLeaveEvent(Player player, SpleefGame game, LeaveReason reason) {
        this.player = player;
        this.game = game;
        this.reason = reason;
    }

    /** The player who left. */
    public Player getPlayer() {
        return player;
    }

    /** The game the player left. */
    public SpleefGame getGame() {
        return game;
    }

    /** The reason the player left. */
    public LeaveReason getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
