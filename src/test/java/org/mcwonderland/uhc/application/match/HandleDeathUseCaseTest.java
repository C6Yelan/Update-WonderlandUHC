package org.mcwonderland.uhc.application.match;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HandleDeathUseCaseTest {

    @Test
    public void findsWinnerWhenOnlyOneTeamIsAlive() {
        HandleDeathUseCase useCase = new HandleDeathUseCase();
        String aliveTeam = "blue";

        HandleDeathResult<String> result = useCase.evaluate(Arrays.asList(
                HandleDeathUseCase.TeamStatus.of("red", true),
                HandleDeathUseCase.TeamStatus.of(aliveTeam, false)
        ));

        assertTrue(result.hasWinner());
        assertSame(aliveTeam, result.getWinner());
    }

    @Test
    public void hasNoWinnerWhenMultipleTeamsAreAlive() {
        HandleDeathUseCase useCase = new HandleDeathUseCase();

        HandleDeathResult<String> result = useCase.evaluate(Arrays.asList(
                HandleDeathUseCase.TeamStatus.of("red", false),
                HandleDeathUseCase.TeamStatus.of("blue", false)
        ));

        assertFalse(result.hasWinner());
    }

    @Test
    public void hasNoWinnerWhenNoTeamIsAlive() {
        HandleDeathUseCase useCase = new HandleDeathUseCase();

        HandleDeathResult<String> result = useCase.evaluate(Arrays.asList(
                HandleDeathUseCase.TeamStatus.of("red", true),
                HandleDeathUseCase.TeamStatus.of("blue", true)
        ));

        assertFalse(result.hasWinner());
    }

    @Test
    public void hasNoWinnerWhenThereAreNoTeams() {
        HandleDeathResult<String> result = new HandleDeathUseCase().evaluate(Collections.emptyList());

        assertFalse(result.hasWinner());
    }

    @Test(expected = IllegalArgumentException.class)
    public void evaluateRequiresTeams() {
        new HandleDeathUseCase().evaluate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void evaluateRejectsNullTeamStatus() {
        new HandleDeathUseCase().evaluate(Collections.singletonList(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void teamStatusRequiresTeam() {
        HandleDeathUseCase.TeamStatus.of(null, false);
    }
}
