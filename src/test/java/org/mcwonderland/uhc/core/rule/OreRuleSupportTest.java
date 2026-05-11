package org.mcwonderland.uhc.core.rule;

import org.bukkit.Material;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class OreRuleSupportTest {

    @Test
    public void canonicalLimitedOreGroupsDeepVariantsWithBaseOre() {
        assertSame(Material.DIAMOND_ORE, OreRuleSupport.canonicalLimitedOre(Material.DEEPSLATE_DIAMOND_ORE));
        assertSame(Material.GOLD_ORE, OreRuleSupport.canonicalLimitedOre(Material.DEEPSLATE_GOLD_ORE));
        assertSame(Material.IRON_ORE, OreRuleSupport.canonicalLimitedOre(Material.DEEPSLATE_IRON_ORE));
    }

    @Test
    public void configuredNormalOreMatchesDeepVariant() {
        assertTrue(OreRuleSupport.matchesAnyBlock(
                Arrays.asList(Material.DIAMOND_ORE, Material.GOLD_ORE),
                Material.DEEPSLATE_GOLD_ORE));
    }

    @Test
    public void unrelatedConfiguredOreDoesNotMatchDifferentOreFamily() {
        assertFalse(OreRuleSupport.matchesBlock(Material.DIAMOND_ORE, Material.DEEPSLATE_IRON_ORE));
    }

    @Test
    public void diamondOreFamilyIncludesDeepVariant() {
        assertTrue(OreRuleSupport.isDiamondOre(Material.DIAMOND_ORE));
        assertTrue(OreRuleSupport.isDiamondOre(Material.DEEPSLATE_DIAMOND_ORE));
        assertFalse(OreRuleSupport.isDiamondOre(Material.GOLD_ORE));
    }

    @Test
    public void goldOreFamilyIncludesDeepVariant() {
        assertTrue(OreRuleSupport.isGoldOre(Material.GOLD_ORE));
        assertTrue(OreRuleSupport.isGoldOre(Material.DEEPSLATE_GOLD_ORE));
        assertFalse(OreRuleSupport.isGoldOre(Material.DIAMOND_ORE));
    }

    @Test
    public void diamondLessDropsIncludeGemAndSilkTouchOreItems() {
        assertTrue(OreRuleSupport.isDiamondDrop(Material.DIAMOND));
        assertTrue(OreRuleSupport.isDiamondDrop(Material.DIAMOND_ORE));
        assertTrue(OreRuleSupport.isDiamondDrop(Material.DEEPSLATE_DIAMOND_ORE));
        assertFalse(OreRuleSupport.isDiamondDrop(Material.RAW_GOLD));
    }

    @Test
    public void goldLessDropsIncludeRawGoldAndSilkTouchOreItems() {
        assertTrue(OreRuleSupport.isGoldDrop(Material.RAW_GOLD));
        assertTrue(OreRuleSupport.isGoldDrop(Material.GOLD_ORE));
        assertTrue(OreRuleSupport.isGoldDrop(Material.DEEPSLATE_GOLD_ORE));
        assertFalse(OreRuleSupport.isGoldDrop(Material.GOLD_INGOT));
    }

    @Test
    public void cutCleanOreDropsCookRawAndSilkTouchOreItems() {
        assertSame(Material.IRON_INGOT, OreRuleSupport.cookedOreDrop(Material.RAW_IRON));
        assertSame(Material.GOLD_INGOT, OreRuleSupport.cookedOreDrop(Material.RAW_GOLD));
        assertSame(Material.IRON_INGOT, OreRuleSupport.cookedOreDrop(Material.DEEPSLATE_IRON_ORE));
        assertSame(Material.GOLD_INGOT, OreRuleSupport.cookedOreDrop(Material.DEEPSLATE_GOLD_ORE));
    }

    @Test
    public void cutCleanKeepsUnrelatedDropsUnchanged() {
        assertSame(Material.DIAMOND, OreRuleSupport.cookedOreDrop(Material.DIAMOND));
    }
}
