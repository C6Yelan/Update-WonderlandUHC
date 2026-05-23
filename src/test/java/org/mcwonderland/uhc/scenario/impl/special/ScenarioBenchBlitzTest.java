package org.mcwonderland.uhc.scenario.impl.special;

import org.bukkit.Material;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScenarioBenchBlitzTest {

    @Test
    public void craftingTableMatcherHandlesCrafterResultsAndMissingItems() {
        assertTrue(ScenarioBenchBlitz.isCraftingTableType(Material.CRAFTING_TABLE));
        assertFalse(ScenarioBenchBlitz.isCraftingTableType(Material.AIR));
        assertFalse(ScenarioBenchBlitz.isCraftingTableType(null));
    }
}
