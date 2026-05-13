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

    @Test
    public void lethalDamageIsLargeEnoughToKeepOriginalDamageEvent() {
        assertEquals(2048.0D, ScenarioDamageDogers.calculateLethalDamage(20.0D), 0.0D);
        assertEquals(4096.0D, ScenarioDamageDogers.calculateLethalDamage(4096.0D), 0.0D);
        assertEquals(2048.0D, ScenarioDamageDogers.calculateLethalDamage(-1.0D), 0.0D);
    }
}
