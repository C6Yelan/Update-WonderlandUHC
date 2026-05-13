package org.mcwonderland.uhc.scenario.impl.death;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScenarioSwapInventoryTest {

    @Test
    public void swappedDropsReplaceDeathPlayerItemsWithKillerItems() {
        List<String> drops = new ArrayList<>(Arrays.asList("victim sword", "victim apple"));

        List<String> swapped = ScenarioSwapInventory.buildSwappedDrops(
                drops,
                new String[] { "victim sword", "victim apple" },
                new String[] { "killer bow" },
                ScenarioSwapInventoryTest::isDropItem);

        assertEquals(1, swapped.size());
        assertEquals("killer bow", swapped.get(0));
    }

    @Test
    public void swappedDropsDoesNotMutateOriginalDropList() {
        List<String> drops = new ArrayList<>(Arrays.asList("victim sword"));

        ScenarioSwapInventory.buildSwappedDrops(
                drops,
                new String[] { "victim sword" },
                new String[] { "killer bow" },
                ScenarioSwapInventoryTest::isDropItem);

        assertEquals(Arrays.asList("victim sword"), drops);
    }

    @Test
    public void swappedDropsIgnoreEmptyKillerInventorySlots() {
        List<String> drops = new ArrayList<>(Arrays.asList("victim sword"));

        List<String> swapped = ScenarioSwapInventory.buildSwappedDrops(
                drops,
                new String[] { "victim sword" },
                new String[] { null, "AIR", "killer bow" },
                ScenarioSwapInventoryTest::isDropItem);

        assertEquals(1, swapped.size());
        assertEquals("killer bow", swapped.get(0));
    }

    @Test
    public void swappedDropsAreReleasedOnlyWhenTimeBombIsDisabled() {
        assertTrue(ScenarioSwapInventory.shouldReleaseDropsImmediately(false));
        assertFalse(ScenarioSwapInventory.shouldReleaseDropsImmediately(true));
    }

    private static boolean isDropItem(String item) {
        return item != null && !"AIR".equals(item);
    }
}
