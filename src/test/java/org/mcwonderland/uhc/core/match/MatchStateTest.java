package org.mcwonderland.uhc.core.match;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MatchStateTest {

    @Test
    public void waitingIsLobbyOnly() {
        assertTrue(MatchState.WAITING.isLobby());
        assertFalse(MatchState.WAITING.isTeleport());
        assertFalse(MatchState.WAITING.isStarted());
    }

    @Test
    public void teleportingAndPreStartAreTeleportPhases() {
        assertFalse(MatchState.TELEPORTING.isLobby());
        assertTrue(MatchState.TELEPORTING.isTeleport());
        assertFalse(MatchState.TELEPORTING.isStarted());

        assertFalse(MatchState.PRE_START.isLobby());
        assertTrue(MatchState.PRE_START.isTeleport());
        assertFalse(MatchState.PRE_START.isStarted());
    }

    @Test
    public void playingIsStartedOnly() {
        assertFalse(MatchState.PLAYING.isLobby());
        assertFalse(MatchState.PLAYING.isTeleport());
        assertTrue(MatchState.PLAYING.isStarted());
        assertFalse(MatchState.PLAYING.isEnded());
    }

    @Test
    public void endingIsEndedOnly() {
        assertFalse(MatchState.ENDING.isLobby());
        assertFalse(MatchState.ENDING.isTeleport());
        assertFalse(MatchState.ENDING.isStarted());
        assertTrue(MatchState.ENDING.isEnded());
    }

    @Test
    public void statesAdvanceInMatchLifecycleOrder() {
        assertSame(MatchState.TELEPORTING, MatchState.WAITING.nextState());
        assertSame(MatchState.PRE_START, MatchState.TELEPORTING.nextState());
        assertSame(MatchState.PLAYING, MatchState.PRE_START.nextState());
        assertSame(MatchState.ENDING, MatchState.PLAYING.nextState());
    }

    @Test(expected = IllegalStateException.class)
    public void endingDoesNotHaveNextState() {
        MatchState.ENDING.nextState();
    }
}
