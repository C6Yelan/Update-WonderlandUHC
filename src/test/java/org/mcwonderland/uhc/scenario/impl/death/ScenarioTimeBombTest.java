package org.mcwonderland.uhc.scenario.impl.death;

import org.bukkit.Material;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScenarioTimeBombTest {

    @Test
    public void countStoredDropsUsesAvailableInventorySize() {
        assertEquals(27, ScenarioTimeBomb.countStoredDrops(80, 27));
        assertEquals(54, ScenarioTimeBomb.countStoredDrops(80, 54));
        assertEquals(3, ScenarioTimeBomb.countStoredDrops(3, 54));
    }

    @Test
    public void extractOverflowDropsClearsDeathEventDrops() {
        List<String> drops = new ArrayList<>(Arrays.asList("one", "two", "three", "four"));

        List<String> overflow = ScenarioTimeBomb.extractOverflowDrops(drops, 2);

        assertEquals(Arrays.asList("three", "four"), overflow);
        assertEquals(0, drops.size());
    }

    @Test
    public void extractOverflowDropsReturnsEmptyWhenStoredCountIsTooLarge() {
        List<String> drops = new ArrayList<>(Arrays.asList("one", "two"));

        List<String> overflow = ScenarioTimeBomb.extractOverflowDrops(drops, 10);

        assertEquals(0, overflow.size());
        assertEquals(0, drops.size());
    }

    @Test
    public void storableMaterialSkipsNullAndAir() {
        assertFalse(ScenarioTimeBomb.isStorableMaterial(null));
        assertFalse(ScenarioTimeBomb.isStorableMaterial(Material.AIR));
        assertTrue(ScenarioTimeBomb.isStorableMaterial(Material.DIAMOND));
    }
}
