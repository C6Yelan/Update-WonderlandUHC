package org.mcwonderland.uhc.application.match;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class MatchTimerTickUseCaseTest {

    @Test
    public void stoppedTimerDoesNotAdvanceClockOrRunTimers() {
        MatchTimerTickResult result = new MatchTimerTickUseCase().advance(false, 7, 12);

        assertFalse(result.shouldRunTimers());
        assertEquals(7, result.getExecutionTick());
        assertEquals(12, result.getTotalSecond());
    }

    @Test
    public void runningTimerAdvancesTickWithinSameSecond() {
        MatchTimerTickResult result = new MatchTimerTickUseCase().advance(true, 7, 12);

        assertTrue(result.shouldRunTimers());
        assertEquals(7, result.getExecutionTick());
        assertEquals(12, result.getTotalSecond());
    }

    @Test
    public void runningTimerRollsOverAfterTwentyTicks() {
        MatchTimerTickResult result = new MatchTimerTickUseCase().advance(true, 20, 12);

        assertTrue(result.shouldRunTimers());
        assertEquals(0, result.getExecutionTick());
        assertEquals(13, result.getTotalSecond());
    }
}
