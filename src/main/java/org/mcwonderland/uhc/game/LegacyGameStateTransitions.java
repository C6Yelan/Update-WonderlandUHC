package org.mcwonderland.uhc.game;

import org.mcwonderland.uhc.application.match.MatchTransition;

final class LegacyGameStateTransitions {

    private LegacyGameStateTransitions() {
    }

    static MatchTransition transitionFor(StateName stateName) {
        if (stateName == null)
            throw new IllegalArgumentException("stateName cannot be null.");

        switch (stateName) {
            case WAITING:
                return MatchTransition.START_TELEPORTING;
            case TELEPORTING:
                return MatchTransition.FINISH_TELEPORTING;
            case PRE_START:
                return MatchTransition.START_PLAYING;
            case PLAYING:
                throw new IllegalStateException("Legacy Game does not have an ENDING state transition.");
            default:
                throw new IllegalArgumentException("Unknown legacy game state: " + stateName + ".");
        }
    }
}
