package org.mcwonderland.uhc.scenario.impl.damage;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ScenarioDamageDogersTest {

    @After
    public void tearDown() {
        ScenarioDamageDogers.resetNumberOfDead();
    }

    @Test
    public void resetNumberOfDeadClearsPreviousVictims() {
        assertEquals(2, ScenarioDamageDogers.recordDeathAndGetRemaining(3));
        assertEquals(1, ScenarioDamageDogers.getNumberOfDead());

        ScenarioDamageDogers.resetNumberOfDead();

        assertEquals(0, ScenarioDamageDogers.getNumberOfDead());
        assertEquals(2, ScenarioDamageDogers.recordDeathAndGetRemaining(3));
    }
}
