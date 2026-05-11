package org.mcwonderland.uhc.scenario.impl.death;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScenarioShiftKillTest {

    @Test
    public void penaltyDamageUsesHalfOfHealthAndAbsorption() {
        assertEquals(12.0, ScenarioShiftKill.calculatePenaltyDamage(20.0, 4.0), 0.0001);
    }

    @Test
    public void penaltyDamageIgnoresNegativeAbsorption() {
        assertEquals(10.0, ScenarioShiftKill.calculatePenaltyDamage(20.0, -4.0), 0.0001);
    }

    @Test
    public void penaltyDamageNeverGoesBelowZero() {
        assertEquals(0.0, ScenarioShiftKill.calculatePenaltyDamage(-10.0, -4.0), 0.0001);
    }
}
