package org.mcwonderland.uhc.scenario.impl.block;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScenarioBloodDiamondsTest {

    @Test
    public void damageAmountUsesConfiguredDamageForEachDiamondOre() {
        assertEquals(16.0D, ScenarioBloodDiamonds.calculateDamageAmount(1, 16), 0.0D);
        assertEquals(12.0D, ScenarioBloodDiamonds.calculateDamageAmount(3, 4), 0.0D);
    }

    @Test
    public void damageAmountDefaultsToOneDamage() {
        assertEquals(3.0D, ScenarioBloodDiamonds.calculateDamageAmount(null, 3), 0.0D);
    }

    @Test
    public void damageAmountKeepsMinimumOreCountAtOne() {
        assertEquals(2.0D, ScenarioBloodDiamonds.calculateDamageAmount(2, 0), 0.0D);
    }
}
