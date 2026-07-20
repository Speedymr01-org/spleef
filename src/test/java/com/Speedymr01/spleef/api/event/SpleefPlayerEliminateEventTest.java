package com.Speedymr01.spleef.api.event;

import com.Speedymr01.spleef.game.SpleefGame;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static com.Speedymr01.spleef.TestStubs.player;
import static org.junit.jupiter.api.Assertions.*;

class SpleefPlayerEliminateEventTest {

    @Test
    void constructor_setsPlayerAndGame() {
        Player p = player("Eliminated");
        SpleefPlayerEliminateEvent event = new SpleefPlayerEliminateEvent(p, null, false);
        assertSame(p, event.getPlayer());
        assertNull(event.getGame());
        assertFalse(event.isTeamEliminated());
    }

    @Test
    void constructor_teamEliminated() {
        Player p = player("LastOfTeam");
        SpleefPlayerEliminateEvent event = new SpleefPlayerEliminateEvent(p, null, true);
        assertTrue(event.isTeamEliminated());
    }

    @Test
    void constructor_notTeamEliminated() {
        Player p = player("Alone");
        SpleefPlayerEliminateEvent event = new SpleefPlayerEliminateEvent(p, null, false);
        assertFalse(event.isTeamEliminated());
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        SpleefPlayerEliminateEvent event = new SpleefPlayerEliminateEvent(player("X"), null, false);
        assertNotNull(event.getHandlers());
        assertSame(SpleefPlayerEliminateEvent.getHandlerList(), event.getHandlers());
    }
}
