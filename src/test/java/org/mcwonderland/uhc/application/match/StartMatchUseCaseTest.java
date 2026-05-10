package org.mcwonderland.uhc.application.match;

import org.junit.Test;
import org.mcwonderland.uhc.core.match.MatchState;
import org.mcwonderland.uhc.core.match.UhcMatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class StartMatchUseCaseTest {

    @Test
    public void startMovesWaitingMatchToTeleporting() {
        UhcMatch match = UhcMatch.create();

        MatchTransitionResult result = new StartMatchUseCase().start(match);

        assertTrue(result.isSuccess());
        assertSame(MatchState.WAITING, result.getSourceState());
        assertSame(MatchState.TELEPORTING, result.getTargetState());
        assertSame(MatchState.TELEPORTING, match.getState());
    }

    @Test
    public void startFailsWhenMatchAlreadyStarted() {
        UhcMatch match = UhcMatch.create();
        match.advanceState();

        MatchTransitionResult result = new StartMatchUseCase().start(match);

        assertFalse(result.isSuccess());
        assertSame(MatchState.TELEPORTING, result.getSourceState());
        assertSame(MatchState.TELEPORTING, match.getState());
    }
}
