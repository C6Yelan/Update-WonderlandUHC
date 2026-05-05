package org.mcwonderland.uhc.core.match;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class MatchRepositoryTest {

    @Test
    public void repositoryStartsWithoutActiveMatch() {
        MatchRepository repository = new MatchRepository();

        assertEquals(null, repository.getActiveMatch());
    }

    @Test
    public void defaultMatchCanBeCreatedAndStored() {
        MatchRepository repository = new MatchRepository();

        UhcMatch match = repository.createDefaultMatch();

        assertSame(match, repository.getActiveMatch());
        assertSame(MatchState.WAITING, match.getState());
    }

    @Test
    public void activeMatchCanBeReplaced() {
        MatchRepository repository = new MatchRepository();
        UhcMatch match = UhcMatch.create();

        repository.setActiveMatch(match);

        assertSame(match, repository.getActiveMatch());
    }

    @Test
    public void activeMatchCanBeCleared() {
        MatchRepository repository = new MatchRepository();
        repository.createDefaultMatch();

        repository.clearActiveMatch();

        assertEquals(null, repository.getActiveMatch());
    }

    @Test(expected = IllegalArgumentException.class)
    public void activeMatchCannotBeSetToNull() {
        MatchRepository repository = new MatchRepository();

        repository.setActiveMatch(null);
    }
}
