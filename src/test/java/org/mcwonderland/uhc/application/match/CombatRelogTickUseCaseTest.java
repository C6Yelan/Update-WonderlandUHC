package org.mcwonderland.uhc.application.match;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CombatRelogTickUseCaseTest {

    @Test
    public void relogExpiresWhenNoSecondsRemain() {
        CombatRelogTickResult result = new CombatRelogTickUseCase().tick(0, false, true);

        assertSame(CombatRelogTickAction.EXPIRE, result.getAction());
        assertEquals(-1, result.getNextRemainingSeconds());
    }

    @Test
    public void relogDamagesEntityWhenBorderMovesAndEntityIsOutside() {
        CombatRelogTickResult result = new CombatRelogTickUseCase().tick(12, true, false);

        assertSame(CombatRelogTickAction.DAMAGE_OUTSIDE_BORDER, result.getAction());
        assertEquals(11, result.getNextRemainingSeconds());
    }

    @Test
    public void relogWaitsWhenBorderIsNotMoving() {
        CombatRelogTickResult result = new CombatRelogTickUseCase().tick(12, false, false);

        assertSame(CombatRelogTickAction.WAIT, result.getAction());
        assertEquals(11, result.getNextRemainingSeconds());
    }

    @Test
    public void relogWaitsWhenEntityIsInsideMovingBorder() {
        CombatRelogTickResult result = new CombatRelogTickUseCase().tick(12, true, true);

        assertSame(CombatRelogTickAction.WAIT, result.getAction());
        assertEquals(11, result.getNextRemainingSeconds());
    }
}
