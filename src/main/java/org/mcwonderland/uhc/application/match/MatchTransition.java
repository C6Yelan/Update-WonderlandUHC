package org.mcwonderland.uhc.application.match;

import org.mcwonderland.uhc.core.match.MatchState;

public enum MatchTransition {
    START_TELEPORTING(MatchState.WAITING, MatchState.TELEPORTING),
    FINISH_TELEPORTING(MatchState.TELEPORTING, MatchState.PRE_START),
    START_PLAYING(MatchState.PRE_START, MatchState.PLAYING),
    END_MATCH(MatchState.PLAYING, MatchState.ENDING);

    private final MatchState sourceState;
    private final MatchState targetState;

    MatchTransition(MatchState sourceState, MatchState targetState) {
        this.sourceState = sourceState;
        this.targetState = targetState;
    }

    public MatchState getSourceState() {
        return sourceState;
    }

    public MatchState getTargetState() {
        return targetState;
    }

    public static MatchTransition fromSourceState(MatchState sourceState) {
        if (sourceState == null)
            throw new IllegalArgumentException("sourceState cannot be null.");

        for (MatchTransition transition : values())
            if (transition.sourceState == sourceState)
                return transition;

        throw new IllegalStateException("Match state " + sourceState + " does not have a transition.");
    }
}
