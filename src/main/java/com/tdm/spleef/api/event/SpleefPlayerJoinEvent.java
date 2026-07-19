package com.tdm.spleef.api.event;

import com.tdm.spleef.game.SpleefGame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player joins a Spleef game.
 */
public class SpleefPlayerJoinEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SpleefGame game;

    public SpleefPlayerJoinEvent(Player player, SpleefGame game) {
        this.player = player;
        this.game = game;
    }

    /** The player who joined. */
    public Player getPlayer() {
        return player;
    }

    /** The game the player joined. */
    public SpleefGame getGame() {
        return game;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
