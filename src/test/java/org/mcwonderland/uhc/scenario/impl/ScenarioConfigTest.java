package org.mcwonderland.uhc.scenario.impl;

import org.bukkit.Material;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ScenarioConfigTest {

    @Test
    public void materialParserRequiresCurrentBukkitNames() {
        assertEquals(Material.CRAFTING_TABLE, ScenarioConfig.parseMaterial("CRAFTING_TABLE", "Bench_Blitz", "Type"));
        assertEquals(Material.COBWEB, ScenarioConfig.parseMaterial("COBWEB", "Silk_Web", "Type"));
        assertEquals(Material.MUSHROOM_STEW, ScenarioConfig.parseMaterial("MUSHROOM_STEW", "Soup", "Type"));
        assertEquals(Material.ENCHANTING_TABLE, ScenarioConfig.parseMaterial("ENCHANTING_TABLE", "No_Enchant", "Type"));
        assertEquals(Material.COOKED_COD, ScenarioConfig.parseMaterial("COOKED_COD", "Food_Neophobia", "Type"));
    }

    @Test
    public void legacyMaterialAliasesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScenarioConfig.parseMaterial("WORKBENCH", "Bench_Blitz", "Type"));
        assertThrows(IllegalArgumentException.class, () -> ScenarioConfig.parseMaterial("WEB", "Silk_Web", "Type"));
        assertThrows(IllegalArgumentException.class, () -> ScenarioConfig.parseMaterial("MUSHROOM_SOUP", "Soup", "Type"));
        assertThrows(IllegalArgumentException.class, () -> ScenarioConfig.parseMaterial("ENCHANTMENT_TABLE", "No_Enchant", "Type"));
        assertThrows(IllegalArgumentException.class, () -> ScenarioConfig.parseMaterial("COOKED_FISH", "Food_Neophobia", "Type"));
    }

    @Test
    public void materialParserAcceptsNamespacedAndLooseValues() {
        assertEquals(Material.DIAMOND_ORE, ScenarioConfig.parseMaterial("minecraft:diamond_ore", "Double_Or_Nothing", "Trigger_Blocks"));
        assertEquals(Material.DIAMOND_ORE, ScenarioConfig.parseMaterial("diamond-ore", "Double_Or_Nothing", "Trigger_Blocks"));
        assertEquals(Material.DIAMOND_ORE, ScenarioConfig.parseMaterial("diamond ore", "Double_Or_Nothing", "Trigger_Blocks"));
    }

    @Test
    public void materialListParserUsesCurrentNames() {
        assertEquals(
                Arrays.asList(Material.DIAMOND_ORE, Material.CRAFTING_TABLE, Material.COBWEB),
                ScenarioConfig.parseMaterialList(Arrays.asList("DIAMOND_ORE", "CRAFTING_TABLE", "COBWEB"), "Double_Or_Nothing", "Trigger_Blocks"));
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
