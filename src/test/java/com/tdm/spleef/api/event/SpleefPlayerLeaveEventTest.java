package com.tdm.spleef.api.event;

import com.tdm.spleef.api.event.SpleefPlayerLeaveEvent.LeaveReason;
import com.tdm.spleef.game.SpleefGame;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static com.tdm.spleef.TestStubs.player;
import static org.junit.jupiter.api.Assertions.*;

class SpleefPlayerLeaveEventTest {

    @Test
    void constructor_setsPlayerAndGame() {
        Player p = player("Leaver");
        SpleefPlayerLeaveEvent event = new SpleefPlayerLeaveEvent(p, null, LeaveReason.LEAVE);
        assertSame(p, event.getPlayer());
        assertNull(event.getGame());
        assertEquals(LeaveReason.LEAVE, event.getReason());
    }

    @Test
    void constructor_eliminatedReason() {
        Player p = player("Eliminated");
        SpleefPlayerLeaveEvent event = new SpleefPlayerLeaveEvent(p, null, LeaveReason.ELIMINATED);
        assertEquals(LeaveReason.ELIMINATED, event.getReason());
    }

    @Test
    void constructor_gameEndReason() {
        Player p = player("Done");
        SpleefPlayerLeaveEvent event = new SpleefPlayerLeaveEvent(p, null, LeaveReason.GAME_END);
        assertEquals(LeaveReason.GAME_END, event.getReason());
    }

    @Test
    void leaveReason_enumValues() {
        assertEquals(3, LeaveReason.values().length);
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        SpleefPlayerLeaveEvent event = new SpleefPlayerLeaveEvent(player("X"), null, LeaveReason.LEAVE);
        assertNotNull(event.getHandlers());
        assertSame(SpleefPlayerLeaveEvent.getHandlerList(), event.getHandlers());
    }
}
