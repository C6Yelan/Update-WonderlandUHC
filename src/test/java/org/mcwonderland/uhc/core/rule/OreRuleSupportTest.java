package org.mcwonderland.uhc.core.rule;

import org.bukkit.Material;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OreRuleSupportTest {

    @Test
    public void cookedOreDropIncludesCopperVariants() {
        assertEquals(Material.COPPER_INGOT, OreRuleSupport.cookedOreDrop(Material.COPPER_ORE));
        assertEquals(Material.COPPER_INGOT, OreRuleSupport.cookedOreDrop(Material.DEEPSLATE_COPPER_ORE));
        assertEquals(Material.COPPER_INGOT, OreRuleSupport.cookedOreDrop(Material.RAW_COPPER));
    }
}
