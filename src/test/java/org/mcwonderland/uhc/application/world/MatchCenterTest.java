package org.mcwonderland.uhc.application.world;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MatchCenterTest {

    @Test
    public void matchCenterStoresCoordinatesAndBorderSize() {
        MatchCenter center = new MatchCenter(384, -512, 2000);

        assertEquals(384, center.getX());
        assertEquals(-512, center.getZ());
        assertEquals(2000, center.getBorderSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void matchCenterRejectsNonPositiveBorderSize() {
        new MatchCenter(0, 0, 0);
    }

    @Test
    public void matchCentersCompareByValue() {
        assertEquals(new MatchCenter(384, -512, 2000), new MatchCenter(384, -512, 2000));
        assertNotEquals(new MatchCenter(384, -512, 2000), new MatchCenter(384, -512, 3000));
    }
}
