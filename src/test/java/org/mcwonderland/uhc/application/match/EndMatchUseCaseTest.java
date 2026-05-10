package org.mcwonderland.uhc.application.match;

import org.junit.Test;
import org.mcwonderland.uhc.core.match.MatchState;
import org.mcwonderland.uhc.core.match.UhcMatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class EndMatchUseCaseTest {

    @Test
    public void endMovesPlayingMatchToEnding() {
        UhcMatch match = playingMatch();

        MatchTransitionResult result = new EndMatchUseCase().end(match);

        assertTrue(result.isSuccess());
        assertSame(MatchState.PLAYING, result.getSourceState());
        assertSame(MatchState.ENDING, result.getTargetState());
        assertSame(MatchState.ENDING, match.getState());
    }

    @Test
    public void endIsIdempotentWhenMatchAlreadyEnded() {
        UhcMatch match = playingMatch();
        match.advanceState();

        MatchTransitionResult result = new EndMatchUseCase().end(match);

        assertTrue(result.isSuccess());
        assertSame(MatchState.ENDING, result.getSourceState());
        assertSame(MatchState.ENDING, result.getTargetState());
        assertSame(MatchState.ENDING, match.getState());
    }

    @Test
    public void endFailsWhenMatchIsNotPlaying() {
        UhcMatch match = UhcMatch.create();

        MatchTransitionResult result = new EndMatchUseCase().end(match);

        assertFalse(result.isSuccess());
        assertSame(MatchState.WAITING, result.getSourceState());
        assertSame(MatchState.WAITING, match.getState());
    }

    @Test(expected = IllegalArgumentException.class)
    public void endRequiresMatch() {
        new EndMatchUseCase().end(null);
    }

    private UhcMatch playingMatch() {
        UhcMatch match = UhcMatch.create();
        match.advanceState();
        match.advanceState();
        match.advanceState();
        return match;
    }
}
