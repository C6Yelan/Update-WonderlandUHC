package org.mcwonderland.uhc.core.match;

import org.junit.Test;
import org.mcwonderland.uhc.game.StateName;

import static org.junit.Assert.assertSame;

public class LegacyMatchStateMapperTest {

    @Test
    public void mapsLegacyStateNameToMatchState() {
        assertSame(MatchState.WAITING, LegacyMatchStateMapper.fromStateName(StateName.WAITING));
        assertSame(MatchState.TELEPORTING, LegacyMatchStateMapper.fromStateName(StateName.TELEPORTING));
        assertSame(MatchState.PRE_START, LegacyMatchStateMapper.fromStateName(StateName.PRE_START));
        assertSame(MatchState.PLAYING, LegacyMatchStateMapper.fromStateName(StateName.PLAYING));
    }

    @Test
    public void mapsMatchStateToLegacyStateName() {
        assertSame(StateName.WAITING, LegacyMatchStateMapper.toStateName(MatchState.WAITING));
        assertSame(StateName.TELEPORTING, LegacyMatchStateMapper.toStateName(MatchState.TELEPORTING));
        assertSame(StateName.PRE_START, LegacyMatchStateMapper.toStateName(MatchState.PRE_START));
        assertSame(StateName.PLAYING, LegacyMatchStateMapper.toStateName(MatchState.PLAYING));
    }
}
