package org.mcwonderland.uhc.application.match;

import java.util.Collection;

public final class HandleDeathUseCase {

    public <T> HandleDeathResult<T> evaluate(Collection<TeamStatus<T>> teams) {
        if (teams == null)
            throw new IllegalArgumentException("teams cannot be null.");

        T winner = null;

        for (TeamStatus<T> team : teams) {
            if (team == null)
                throw new IllegalArgumentException("teams cannot contain null.");

            if (team.isEliminated())
                continue;

            if (winner != null)
                return HandleDeathResult.noWinner();

            winner = team.getTeam();
        }

        if (winner == null)
            return HandleDeathResult.noWinner();

        return HandleDeathResult.winner(winner);
    }

    public static final class TeamStatus<T> {
        private final T team;
        private final boolean eliminated;

        private TeamStatus(T team, boolean eliminated) {
            this.team = team;
            this.eliminated = eliminated;
        }

        public static <T> TeamStatus<T> of(T team, boolean eliminated) {
            if (team == null)
                throw new IllegalArgumentException("team cannot be null.");

            return new TeamStatus<>(team, eliminated);
        }

        public T getTeam() {
            return team;
        }

        public boolean isEliminated() {
            return eliminated;
        }
    }
}
