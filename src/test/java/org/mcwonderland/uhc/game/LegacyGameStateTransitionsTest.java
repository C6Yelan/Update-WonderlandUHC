package org.mcwonderland.uhc.game;

import org.junit.Test;
import org.mcwonderland.uhc.application.match.MatchTransition;

import static org.junit.Assert.assertSame;

public class LegacyGameStateTransitionsTest {

    @Test
    public void mapsWaitingToStartTeleporting() {
        assertSame(MatchTransition.START_TELEPORTING, LegacyGameStateTransitions.transitionFor(StateName.WAITING));
    }

    @Test
    public void mapsTeleportingToFinishTeleporting() {
        assertSame(MatchTransition.FINISH_TELEPORTING, LegacyGameStateTransitions.transitionFor(StateName.TELEPORTING));
    }

    @Test
    public void mapsPreStartToStartPlaying() {
        assertSame(MatchTransition.START_PLAYING, LegacyGameStateTransitions.transitionFor(StateName.PRE_START));
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsPlayingBecauseLegacyGameHasNoNextState() {
        LegacyGameStateTransitions.transitionFor(StateName.PLAYING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullState() {
        LegacyGameStateTransitions.transitionFor(null);
    }
}
