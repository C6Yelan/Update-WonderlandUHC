package org.mcwonderland.uhc.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BorderUtilTest {

    @Test
    public void shrinkSpeedUsesLegacyPerSideBlocksPerSecond() {
        assertEquals(1D, BorderUtil.getShrinkSpeedPerSecond(2000, 1000, 500), 0.00001D);
    }

    @Test
    public void shrinkSecondsUseLegacyPerSideBlocksPerSecond() {
        assertEquals(250, BorderUtil.getShrinkSecondsCost(2000, 1000, 2D));
    }

    @Test
    public void shrinkSecondsMatchLegacyIntegerTruncation() {
        assertEquals(249, BorderUtil.getShrinkSecondsCost(2000, 1001, 2D));
    }
}
