package com.Speedymr01.spleef.api.event;

import com.Speedymr01.spleef.game.SpleefGame;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static com.Speedymr01.spleef.TestStubs.player;
import static org.junit.jupiter.api.Assertions.*;

class SpleefPlayerJoinEventTest {

    @Test
    void constructor_setsPlayerAndGame() {
        Player p = player("Joiner");
        SpleefPlayerJoinEvent event = new SpleefPlayerJoinEvent(p, null);
        assertSame(p, event.getPlayer());
        assertNull(event.getGame());
    }

    @Test
    void constructor_withGame() {
        Player p = player("Test");
        SpleefPlayerJoinEvent event = new SpleefPlayerJoinEvent(p, null);
        assertNotNull(event.getPlayer());
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        SpleefPlayerJoinEvent event = new SpleefPlayerJoinEvent(player("X"), null);
        assertNotNull(event.getHandlers());
        assertSame(SpleefPlayerJoinEvent.getHandlerList(), event.getHandlers());
    }
}
