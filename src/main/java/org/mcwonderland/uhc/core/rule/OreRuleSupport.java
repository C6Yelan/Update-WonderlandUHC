package org.mcwonderland.uhc.core.rule;

import lombok.experimental.UtilityClass;
import org.bukkit.Material;

import java.util.List;

@UtilityClass
public class OreRuleSupport {

    public Material canonicalLimitedOre(Material material) {
        if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE)
            return Material.DIAMOND_ORE;

        if (material == Material.GOLD_ORE || material == Material.DEEPSLATE_GOLD_ORE)
            return Material.GOLD_ORE;

        if (material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE)
            return Material.IRON_ORE;

        return material;
    }

    public boolean matchesAnyBlock(List<Material> configuredBlocks, Material blockType) {
        for (Material configuredBlock : configuredBlocks) {
            if (matchesBlock(configuredBlock, blockType))
                return true;
        }

        return false;
    }

    public boolean matchesBlock(Material configuredBlock, Material blockType) {
        return configuredBlock == blockType
                || canonicalLimitedOre(configuredBlock) == canonicalLimitedOre(blockType);
    }

    public boolean isDiamondOre(Material material) {
        return canonicalLimitedOre(material) == Material.DIAMOND_ORE;
    }

    public boolean isGoldOre(Material material) {
        return canonicalLimitedOre(material) == Material.GOLD_ORE;
    }

    public boolean isDiamondDrop(Material material) {
        return material == Material.DIAMOND
                || material == Material.DIAMOND_ORE
                || material == Material.DEEPSLATE_DIAMOND_ORE;
    }

    public boolean isGoldDrop(Material material) {
        return material == Material.RAW_GOLD
                || material == Material.GOLD_ORE
                || material == Material.DEEPSLATE_GOLD_ORE;
    }

    public Material cookedOreDrop(Material material) {
        if (material == Material.IRON_ORE
                || material == Material.DEEPSLATE_IRON_ORE
                || material == Material.RAW_IRON)
            return Material.IRON_INGOT;

        if (material == Material.GOLD_ORE
                || material == Material.DEEPSLATE_GOLD_ORE
                || material == Material.RAW_GOLD)
            return Material.GOLD_INGOT;

        return material;
    }
}
