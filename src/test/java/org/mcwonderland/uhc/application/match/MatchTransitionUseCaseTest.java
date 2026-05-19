package org.mcwonderland.uhc.application.match;

import org.junit.Test;
import org.mcwonderland.uhc.core.match.MatchState;
import org.mcwonderland.uhc.core.match.UhcMatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MatchTransitionUseCaseTest {

    @Test
    public void transitionsAdvanceMatchInDeclaredOrder() {
        UhcMatch match = UhcMatch.create();
        MatchTransitionUseCase useCase = new MatchTransitionUseCase();

        assertSuccess(useCase.apply(match, MatchTransition.START_TELEPORTING), MatchState.WAITING, MatchState.TELEPORTING);
        assertSame(MatchState.TELEPORTING, match.getState());

        assertSuccess(useCase.apply(match, MatchTransition.FINISH_TELEPORTING), MatchState.TELEPORTING, MatchState.PRE_START);
        assertSame(MatchState.PRE_START, match.getState());

        assertSuccess(useCase.apply(match, MatchTransition.START_PLAYING), MatchState.PRE_START, MatchState.PLAYING);
        assertSame(MatchState.PLAYING, match.getState());

        assertSuccess(useCase.apply(match, MatchTransition.END_MATCH), MatchState.PLAYING, MatchState.ENDING);
        assertSame(MatchState.ENDING, match.getState());
    }

    @Test
    public void transitionsCanBeSelectedFromSourceState() {
        assertSame(MatchTransition.START_TELEPORTING, MatchTransition.fromSourceState(MatchState.WAITING));
        assertSame(MatchTransition.FINISH_TELEPORTING, MatchTransition.fromSourceState(MatchState.TELEPORTING));
        assertSame(MatchTransition.START_PLAYING, MatchTransition.fromSourceState(MatchState.PRE_START));
        assertSame(MatchTransition.END_MATCH, MatchTransition.fromSourceState(MatchState.PLAYING));
    }

    @Test(expected = IllegalStateException.class)
    public void endedMatchStateHasNoTransition() {
        MatchTransition.fromSourceState(MatchState.ENDING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transitionSelectionRequiresSourceState() {
        MatchTransition.fromSourceState(null);
    }

    @Test
    public void transitionFailsWhenCurrentStateDoesNotMatch() {
        UhcMatch match = UhcMatch.create();
        MatchTransitionUseCase useCase = new MatchTransitionUseCase();

        MatchTransitionResult result = useCase.apply(match, MatchTransition.START_PLAYING);

        assertFalse(result.isSuccess());
        assertSame(MatchTransitionStatus.INVALID_SOURCE_STATE, result.getStatus());
        assertSame(MatchState.WAITING, result.getSourceState());
        assertSame(MatchState.PLAYING, result.getTargetState());
        assertEquals("Expected match state PRE_START but was WAITING.", result.getFailureReason());
        assertSame(MatchState.WAITING, match.getState());
    }

    @Test
    public void transitionFailsWhenMatchIsMissing() {
        MatchTransitionResult result = new MatchTransitionUseCase().apply(null, MatchTransition.START_TELEPORTING);

        assertFalse(result.isSuccess());
        assertSame(MatchTransitionStatus.MISSING_MATCH, result.getStatus());
        assertEquals(null, result.getSourceState());
        assertSame(MatchState.TELEPORTING, result.getTargetState());
        assertEquals("Match is required for transition START_TELEPORTING.", result.getFailureReason());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transitionRequiresTransition() {
        new MatchTransitionUseCase().apply(UhcMatch.create(), null);
    }

    private void assertSuccess(MatchTransitionResult result, MatchState sourceState, MatchState targetState) {
        assertTrue(result.isSuccess());
        assertSame(MatchTransitionStatus.SUCCESS, result.getStatus());
        assertSame(sourceState, result.getSourceState());
        assertSame(targetState, result.getTargetState());
        assertEquals(null, result.getFailureReason());
    }
}
