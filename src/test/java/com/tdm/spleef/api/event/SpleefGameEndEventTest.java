package com.tdm.spleef.api.event;

import com.tdm.spleef.game.SpleefGame;
import com.tdm.spleef.game.SpleefGame.GameState;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tdm.spleef.TestStubs.player;
import static org.junit.jupiter.api.Assertions.*;

class SpleefGameEndEventTest {

    @Test
    void constructor_setsAllFields() {
        Player winner = player("Winner");
        List<Player> winners = List.of(winner);

        SpleefGameEndEvent event = new SpleefGameEndEvent(null, winners, GameState.FINISHED);
        assertNull(event.getGame());
        assertEquals(1, event.getWinners().size());
        assertSame(winner, event.getWinners().get(0));
        assertEquals(GameState.FINISHED, event.getFinalState());
    }

    @Test
    void constructor_emptyWinners() {
        SpleefGameEndEvent event = new SpleefGameEndEvent(null, List.of(), GameState.FINISHED);
        assertTrue(event.getWinners().isEmpty());
    }

    @Test
    void constructor_multipleWinners() {
        Player a = player("A");
        Player b = player("B");
        SpleefGameEndEvent event = new SpleefGameEndEvent(null, List.of(a, b), GameState.FINISHED);
        assertEquals(2, event.getWinners().size());
    }

    @Test
    void getWinners_returnsUnmodifiable() {
        SpleefGameEndEvent event = new SpleefGameEndEvent(null, List.of(player("X")), GameState.FINISHED);
        assertThrows(UnsupportedOperationException.class, () -> event.getWinners().add(player("Y")));
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        SpleefGameEndEvent event = new SpleefGameEndEvent(null, List.of(), GameState.FINISHED);
        assertNotNull(event.getHandlers());
        assertSame(SpleefGameEndEvent.getHandlerList(), event.getHandlers());
    }
}
