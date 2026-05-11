package org.mcwonderland.uhc.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VeinMinerTest {

    @Test
    public void nextVisitedCountIncrementsBelowLimit() {
        assertEquals(11, VeinMiner.nextVisitedCount(10, 100));
    }

    @Test
    public void nextVisitedCountStopsAtLimit() {
        assertEquals(100, VeinMiner.nextVisitedCount(100, 100));
    }
}
