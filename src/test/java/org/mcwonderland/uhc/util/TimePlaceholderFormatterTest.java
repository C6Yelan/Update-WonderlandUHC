package org.mcwonderland.uhc.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TimePlaceholderFormatterTest {

    @Before
    public void configureChineseTimeSymbols() {
        TimePlaceholderFormatter.configureSymbols("秒", "秒", "分鐘", "分鐘");
    }

    @After
    public void restoreDefaultTimeSymbols() {
        TimePlaceholderFormatter.configureSymbols("second", "seconds", "minute", "minutes ");
    }

    @Test
    public void formatsClockAndFancyTimePlaceholders() {
        assertEquals("03:05", TimePlaceholderFormatter.clockTime(185));
        assertEquals("1:00:00", TimePlaceholderFormatter.clockTime(3600));
        assertEquals("4:00:00", TimePlaceholderFormatter.clockTime(14400));
        assertEquals("3分鐘5秒", TimePlaceholderFormatter.fancyTime(185));
        assertEquals("1分鐘", TimePlaceholderFormatter.fancyTime(60));
    }

    @Test
    public void exposesReplacementPairsForLegacySimpleReplacer() {
        Object[] pairs = TimePlaceholderFormatter.replacementPairs(185);

        assertArrayEquals(new Object[]{
                "{time}", "03:05",
                "{fancy-time}", "3分鐘5秒",
                "{seconds}", 185,
                "{second}", 185
        }, pairs);
    }
}
