package org.mcwonderland.uhc.scenario.impl.death;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ScenarioSwapInventoryTest {

    @Test
    public void swappedDropsReplaceDeathPlayerItemsWithKillerItems() {
        List<String> drops = new ArrayList<>(Arrays.asList("victim sword", "victim apple"));

        List<String> swapped = ScenarioSwapInventory.buildSwappedDrops(
                drops,
                new String[] { "victim sword", "victim apple" },
                new String[] { "killer bow" });

        assertEquals(Arrays.asList("killer bow"), swapped);
    }

    @Test
    public void swappedDropsDoesNotMutateOriginalDropList() {
        List<String> drops = new ArrayList<>(Arrays.asList("victim sword"));

        ScenarioSwapInventory.buildSwappedDrops(
                drops,
                new String[] { "victim sword" },
                new String[] { "killer bow" });

        assertEquals(Arrays.asList("victim sword"), drops);
    }
}
