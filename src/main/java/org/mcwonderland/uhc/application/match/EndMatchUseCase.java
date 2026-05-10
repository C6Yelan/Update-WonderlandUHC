package org.mcwonderland.uhc.application.match;

import org.mcwonderland.uhc.core.match.MatchState;
import org.mcwonderland.uhc.core.match.UhcMatch;

public final class EndMatchUseCase {
    private final MatchTransitionUseCase transitions;

    public EndMatchUseCase() {
        this(new MatchTransitionUseCase());
    }

    EndMatchUseCase(MatchTransitionUseCase transitions) {
        if (transitions == null)
            throw new IllegalArgumentException("transitions cannot be null.");

        this.transitions = transitions;
    }

    public MatchTransitionResult end(UhcMatch match) {
        if (match == null)
            throw new IllegalArgumentException("match cannot be null.");

        if (match.getState() == MatchState.ENDING)
            return MatchTransitionResult.success(MatchState.ENDING, MatchState.ENDING);

        return transitions.apply(match, MatchTransition.END_MATCH);
    }
}
