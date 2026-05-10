package org.mcwonderland.uhc.application.match;

import org.mcwonderland.uhc.core.match.UhcMatch;

public final class StartMatchUseCase {
    private final MatchTransitionUseCase transitions;

    public StartMatchUseCase() {
        this(new MatchTransitionUseCase());
    }

    StartMatchUseCase(MatchTransitionUseCase transitions) {
        if (transitions == null)
            throw new IllegalArgumentException("transitions cannot be null.");

        this.transitions = transitions;
    }

    public MatchTransitionResult start(UhcMatch match) {
        return transitions.apply(match, MatchTransition.START_TELEPORTING);
    }
}
