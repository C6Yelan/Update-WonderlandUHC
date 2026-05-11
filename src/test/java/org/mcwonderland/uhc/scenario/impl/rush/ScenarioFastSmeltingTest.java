package org.mcwonderland.uhc.scenario.impl.rush;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScenarioFastSmeltingTest {

    @Test
    public void boostedBurnTimeNeverGoesBelowZero() {
        assertEquals(0, ScenarioFastSmelting.calculateBoostedBurnTime(( short ) 1, ( short ) 2));
    }

    @Test
    public void boostedBurnTimeSubtractsSpeed() {
        assertEquals(8, ScenarioFastSmelting.calculateBoostedBurnTime(( short ) 10, ( short ) 2));
    }

    @Test
    public void boostedCookTimeAddsSpeed() {
        assertEquals(12, ScenarioFastSmelting.calculateBoostedCookTime(( short ) 10, ( short ) 2));
    }

    @Test
    public void boostedCookTimeDoesNotOverflowShort() {
        assertEquals(Short.MAX_VALUE, ScenarioFastSmelting.calculateBoostedCookTime(Short.MAX_VALUE, ( short ) 2));
    }
}
