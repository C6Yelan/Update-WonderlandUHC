package org.mcwonderland.uhc.platform.material;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PluginMaterials {

    private PluginMaterials() {
    }

    public static Material materialOf(String materialName) {
        return Material.valueOf(materialName);
    }

    public static ItemStack itemOf(String materialName) {
        return new ItemStack(materialOf(materialName));
    }

    public static ItemStack itemOf(String materialName, int amount) {
        return new ItemStack(materialOf(materialName), amount);
    }

    public static boolean isAir(Block block) {
        return block == null || isAir(block.getType());
    }

    public static boolean isAir(Material material) {
        return material == null || material.isAir();
    }

    public static boolean isLeaves(Material material) {
        return material != null && material.name().endsWith("_LEAVES");
    }

    public static boolean isLog(Material material) {
        return material != null && material.name().endsWith("_LOG");
    }

    public static boolean isLongGrass(Material material) {
        if (material == null || material.name().startsWith("POTTED_"))
            return false;

        return material == Material.SHORT_GRASS
                || material == Material.TALL_GRASS
                || material == Material.FERN
                || material == Material.DEAD_BUSH;
    }

    public static boolean isDoublePlant(Material material) {
        return material == Material.SUNFLOWER
                || material == Material.LILAC
                || material == Material.TALL_GRASS
                || material == Material.LARGE_FERN
                || material == Material.ROSE_BUSH
                || material == Material.PEONY
                || material == Material.TALL_SEAGRASS;
    }

    public static ItemStack getFirstItem(Player player, ItemStack item) {
        if (player == null || item == null)
            return null;

        for (ItemStack otherItem : player.getInventory().getContents()) {
            if (otherItem != null && otherItem.isSimilar(item))
                return otherItem;
        }

        return null;
    }
}
