package org.mcwonderland.uhc.stats;

import org.bukkit.Material;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UHCStatsTest {

    @Test
    public void normalAndDeepDiamondOreShareMiningCount() {
        UHCStats stats = new UHCStats();

        assertEquals(1, stats.addOreMined(Material.DIAMOND_ORE));
        assertEquals(2, stats.addOreMined(Material.DEEPSLATE_DIAMOND_ORE));
    }

    @Test
    public void normalAndDeepGoldOreShareMiningCount() {
        UHCStats stats = new UHCStats();

        assertEquals(1, stats.addOreMined(Material.GOLD_ORE));
        assertEquals(2, stats.addOreMined(Material.DEEPSLATE_GOLD_ORE));
    }
}
