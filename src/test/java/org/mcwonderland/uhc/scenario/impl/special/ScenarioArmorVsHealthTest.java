package org.mcwonderland.uhc.scenario.impl.special;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScenarioArmorVsHealthTest {

    @Test
    public void reducedMaxHealthSubtractsArmorDifference() {
        assertEquals(16.0, ScenarioArmorVsHealth.calculateReducedMaxHealth(20.0, 4.0), 0.0001);
    }

    @Test
    public void reducedMaxHealthIgnoresNegativeDifference() {
        assertEquals(20.0, ScenarioArmorVsHealth.calculateReducedMaxHealth(20.0, -4.0), 0.0001);
    }

    @Test
    public void reducedMaxHealthKeepsMinimumHealth() {
        assertEquals(1.0, ScenarioArmorVsHealth.calculateReducedMaxHealth(4.0, 10.0), 0.0001);
    }

    @Test
    public void restoredMaxHealthAddsAppliedReduction() {
        assertEquals(20.0, ScenarioArmorVsHealth.calculateRestoredMaxHealth(16.0, 4.0), 0.0001);
    }

    @Test
    public void restoredMaxHealthIgnoresNegativeReduction() {
        assertEquals(16.0, ScenarioArmorVsHealth.calculateRestoredMaxHealth(16.0, -4.0), 0.0001);
    }
}
