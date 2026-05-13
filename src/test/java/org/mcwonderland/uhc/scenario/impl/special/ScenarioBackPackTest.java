package org.mcwonderland.uhc.scenario.impl.special;

import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScenarioBackPackTest {

    @Test
    public void collectDropItemsSkipsNullSlots() {
        List<ItemStack> drops = ScenarioBackPack.collectDropItems(new ItemStack[] { null });

        assertEquals(0, drops.size());
    }

    @Test
    public void collectDropItemsAcceptsNullContentsArray() {
        List<ItemStack> drops = ScenarioBackPack.collectDropItems(null);

        assertEquals(0, drops.size());
    }

    @Test
    public void backpackItemsReleaseAtDeathLocationWhenNoConsumerScenarioHandlesThem() {
        assertTrue(ScenarioBackPack.shouldReleaseBackpackItemsAtDeathLocation(false, false, true));
    }

    @Test
    public void backpackItemsStayInDropsWhenTimeBombWillConsumeThem() {
        assertFalse(ScenarioBackPack.shouldReleaseBackpackItemsAtDeathLocation(true, false, true));
    }

    @Test
    public void backpackItemsStayWithKillerWhenSwapInventoryCanConsumeThem() {
        assertFalse(ScenarioBackPack.shouldReleaseBackpackItemsAtDeathLocation(false, true, true));
    }
}
