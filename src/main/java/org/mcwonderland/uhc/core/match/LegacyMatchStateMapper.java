package org.mcwonderland.uhc.core.match;

import org.mcwonderland.uhc.game.StateName;

public final class LegacyMatchStateMapper {

    private LegacyMatchStateMapper() {
    }

    public static MatchState fromStateName(StateName stateName) {
        if (stateName == null)
            throw new IllegalArgumentException("stateName cannot be null.");

        return MatchState.valueOf(stateName.name());
    }

    public static StateName toStateName(MatchState matchState) {
        if (matchState == null)
            throw new IllegalArgumentException("matchState cannot be null.");

        return StateName.valueOf(matchState.name());
    }
}
