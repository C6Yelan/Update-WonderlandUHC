package org.mcwonderland.uhc.scenario.impl.damage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScenarioLessBowDamageTest {

    @Test
    public void decreasePercentReducesDamageByConfiguredPercent() {
        assertEquals(7.5D, ScenarioLessBowDamage.applyDecreasePercent(10D, 25), 0.0D);
        assertEquals(2.5D, ScenarioLessBowDamage.applyDecreasePercent(10D, 75), 0.0D);
    }

    @Test
    public void decreasePercentKeepsDefaultFiftyPercentBehavior() {
        assertEquals(5D, ScenarioLessBowDamage.applyDecreasePercent(10D, 50), 0.0D);
    }

    @Test
    public void decreasePercentClampsInvalidValues() {
        assertEquals(10D, ScenarioLessBowDamage.applyDecreasePercent(10D, -10), 0.0D);
        assertEquals(0D, ScenarioLessBowDamage.applyDecreasePercent(10D, 150), 0.0D);
        assertEquals(10D, ScenarioLessBowDamage.applyDecreasePercent(10D, null), 0.0D);
    }
}
