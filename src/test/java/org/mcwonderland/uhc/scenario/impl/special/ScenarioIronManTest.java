package org.mcwonderland.uhc.scenario.impl.special;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScenarioIronManTest {

    @Test
    public void adjustedMaxHealthAddsExtraHeal() {
        assertEquals(24.0, ScenarioIronMan.calculateAdjustedMaxHealth(20.0, 4.0), 0.0001);
    }

    @Test
    public void adjustedMaxHealthSubtractsExtraHeal() {
        assertEquals(20.0, ScenarioIronMan.calculateAdjustedMaxHealth(24.0, -4.0), 0.0001);
    }

    @Test
    public void adjustedMaxHealthKeepsMinimumHealth() {
        assertEquals(1.0, ScenarioIronMan.calculateAdjustedMaxHealth(2.0, -4.0), 0.0001);
    }
}
