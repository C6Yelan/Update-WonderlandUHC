package org.mcwonderland.uhc.scenario.impl;

import org.bukkit.Material;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScenarioConfigTest {

    @Test
    public void materialAliasesResolveLegacyScenarioValues() {
        assertEquals(Material.CRAFTING_TABLE, ScenarioConfig.parseMaterial("WORKBENCH", "Bench_Blitz", "Type"));
        assertEquals(Material.COBWEB, ScenarioConfig.parseMaterial("WEB", "Silk_Web", "Type"));
        assertEquals(Material.MUSHROOM_STEW, ScenarioConfig.parseMaterial("MUSHROOM_SOUP", "Soup", "Type"));
        assertEquals(Material.ENCHANTING_TABLE, ScenarioConfig.parseMaterial("ENCHANTMENT_TABLE", "No_Enchant", "Type"));
        assertEquals(Material.COOKED_COD, ScenarioConfig.parseMaterial("COOKED_FISH", "Food_Neophobia", "Type"));
    }

    @Test
    public void materialParserAcceptsNamespacedAndLooseValues() {
        assertEquals(Material.DIAMOND_ORE, ScenarioConfig.parseMaterial("minecraft:diamond_ore", "Double_Or_Nothing", "Trigger_Blocks"));
        assertEquals(Material.DIAMOND_ORE, ScenarioConfig.parseMaterial("diamond-ore", "Double_Or_Nothing", "Trigger_Blocks"));
        assertEquals(Material.DIAMOND_ORE, ScenarioConfig.parseMaterial("diamond ore", "Double_Or_Nothing", "Trigger_Blocks"));
    }

    @Test
    public void materialListParserAppliesAliasesToEachEntry() {
        assertEquals(
                Arrays.asList(Material.DIAMOND_ORE, Material.CRAFTING_TABLE, Material.COBWEB),
                ScenarioConfig.parseMaterialList(Arrays.asList("DIAMOND_ORE", "WORKBENCH", "WEB"), "Double_Or_Nothing", "Trigger_Blocks"));
    }

    @Test
    public void invalidMaterialReportsScenarioAndPath() {
        try {
            ScenarioConfig.parseMaterial("NOT_A_BLOCK", "Double_Or_Nothing", "Trigger_Blocks");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Double_Or_Nothing.Trigger_Blocks"));
            assertTrue(ex.getMessage().contains("NOT_A_BLOCK"));
            return;
        }

        throw new AssertionError("invalid material should fail");
    }

}
