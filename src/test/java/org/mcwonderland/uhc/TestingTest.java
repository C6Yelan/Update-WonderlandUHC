package org.mcwonderland.uhc;

import org.junit.Test;
import org.mcwonderland.uhc.game.StateName;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestingTest {

    @Test
    public void stateNameClassifiesLobbyTeleportAndStartedPhases() {
        assertTrue(StateName.WAITING.isLobby());
        assertFalse(StateName.WAITING.isTeleport());
        assertFalse(StateName.WAITING.isStarted());

        assertFalse(StateName.TELEPORTING.isLobby());
        assertTrue(StateName.TELEPORTING.isTeleport());
        assertFalse(StateName.TELEPORTING.isStarted());

        assertFalse(StateName.PRE_START.isLobby());
        assertTrue(StateName.PRE_START.isTeleport());
        assertFalse(StateName.PRE_START.isStarted());

        assertFalse(StateName.PLAYING.isLobby());
        assertFalse(StateName.PLAYING.isTeleport());
        assertTrue(StateName.PLAYING.isStarted());
    }

}
