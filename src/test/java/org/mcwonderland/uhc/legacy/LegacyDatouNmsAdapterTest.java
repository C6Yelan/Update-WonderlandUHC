package org.mcwonderland.uhc.legacy;

import org.bukkit.Material;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LegacyDatouNmsAdapterTest {

    @Test
    public void vanillaArmorPointsMatchMinecraftValues() {
        assertEquals(1.0, LegacyDatouNmsAdapter.getVanillaArmorPoints(Material.LEATHER_HELMET), 0.0001);
        assertEquals(8.0, LegacyDatouNmsAdapter.getVanillaArmorPoints(Material.DIAMOND_CHESTPLATE), 0.0001);
        assertEquals(6.0, LegacyDatouNmsAdapter.getVanillaArmorPoints(Material.NETHERITE_LEGGINGS), 0.0001);
        assertEquals(2.0, LegacyDatouNmsAdapter.getVanillaArmorPoints(Material.TURTLE_HELMET), 0.0001);
        assertEquals(5.0, LegacyDatouNmsAdapter.getVanillaArmorPoints(Material.COPPER_CHESTPLATE), 0.0001);
    }

    @Test
    public void vanillaArmorPointsIgnoreNonArmorMaterials() {
        assertEquals(0.0, LegacyDatouNmsAdapter.getVanillaArmorPoints(Material.DIAMOND_PICKAXE), 0.0001);
        assertEquals(0.0, LegacyDatouNmsAdapter.getVanillaArmorPoints(null), 0.0001);
    }

    @Test
    public void unavailableAdapterUsesVanillaArmorFallback() {
        assertEquals(8.0, LegacyDatouNmsAdapter.current().getArmorPoints(Material.DIAMOND_CHESTPLATE), 0.0001);
        assertEquals(5.0, LegacyDatouNmsAdapter.current().getArmorPoints(Material.COPPER_CHESTPLATE), 0.0001);
        assertEquals(0.0, LegacyDatouNmsAdapter.current().getArmorPoints(Material.DIAMOND_PICKAXE), 0.0001);
    }
}
