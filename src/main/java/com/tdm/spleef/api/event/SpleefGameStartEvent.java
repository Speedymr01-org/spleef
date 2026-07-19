package com.tdm.spleef.api.event;

import com.tdm.spleef.game.SpleefGame;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a Spleef game transitions from COUNTDOWN to ACTIVE.
 * At this point players have been given their shovels and the arena has been filled.
 */
public class SpleefGameStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SpleefGame game;

    public SpleefGameStartEvent(SpleefGame game) {
        this.game = game;
    }

    /** The game that started. */
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
