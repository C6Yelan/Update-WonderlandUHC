package org.mcwonderland.uhc.scenario.impl.block;

import org.bukkit.Material;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScenarioDoubleOrNothingTest {

    @Test
    public void silkTouchToolDisablesDoubleOrNothingForConfiguredBlocks() {
        assertFalse(ScenarioDoubleOrNothing.shouldApply(
                Material.DIAMOND_ORE,
                true,
                List.of(Material.DIAMOND_ORE, Material.GOLD_ORE)));
    }

    @Test
    public void normalToolAppliesDoubleOrNothingForConfiguredBlocks() {
        assertTrue(ScenarioDoubleOrNothing.shouldApply(
                Material.GOLD_ORE,
                false,
                List.of(Material.DIAMOND_ORE, Material.GOLD_ORE)));
    }

    @Test
    public void unconfiguredBlocksDoNotApplyDoubleOrNothing() {
        assertFalse(ScenarioDoubleOrNothing.shouldApply(
                Material.IRON_ORE,
                false,
                List.of(Material.DIAMOND_ORE, Material.GOLD_ORE)));
    }
}
