package com.tdm.spleef.api.event;

import com.tdm.spleef.game.SpleefGame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpleefGameStartEventTest {

    @Test
    void constructor_setsGame() {
        SpleefGameStartEvent event = new SpleefGameStartEvent(null);
        assertNull(event.getGame());
    }

    @Test
    void getHandlers_returnsStaticHandlerList() {
        SpleefGameStartEvent event = new SpleefGameStartEvent(null);
        assertNotNull(event.getHandlers());
        assertSame(SpleefGameStartEvent.getHandlerList(), event.getHandlers());
    }
}
