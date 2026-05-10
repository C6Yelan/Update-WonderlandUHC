package org.mcwonderland.uhc.application.match;

import org.mcwonderland.uhc.core.match.MatchState;
import org.mcwonderland.uhc.core.match.UhcMatch;

public final class MatchTransitionUseCase {

    public MatchTransitionResult apply(UhcMatch match, MatchTransition transition) {
        if (transition == null)
            throw new IllegalArgumentException("transition cannot be null.");

        if (match == null) {
            return MatchTransitionResult.failure(
                    MatchTransitionStatus.MISSING_MATCH,
                    null,
                    transition.getTargetState(),
                    "Match is required for transition " + transition.name() + ".");
        }

        MatchState currentState = match.getState();

        if (currentState != transition.getSourceState()) {
            return MatchTransitionResult.failure(
                    MatchTransitionStatus.INVALID_SOURCE_STATE,
                    currentState,
                    transition.getTargetState(),
                    "Expected match state " + transition.getSourceState() + " but was " + currentState + ".");
        }

        MatchState expectedTargetState = currentState.nextState();

        if (expectedTargetState != transition.getTargetState()) {
            return MatchTransitionResult.failure(
                    MatchTransitionStatus.INVALID_TARGET_STATE,
                    currentState,
                    transition.getTargetState(),
                    "Transition " + transition.name() + " declares target state " + transition.getTargetState()
                            + " but match state " + currentState + " advances to " + expectedTargetState + ".");
        }

        MatchState nextState = match.advanceState();
        return MatchTransitionResult.success(currentState, nextState);
    }
}
