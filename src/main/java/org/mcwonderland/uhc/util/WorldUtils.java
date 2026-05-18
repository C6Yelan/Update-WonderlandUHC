package org.mcwonderland.uhc.util;

import lombok.experimental.UtilityClass;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.Settings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;

@UtilityClass
public class WorldUtils {

    public void dropItems(Location l, Collection<ItemStack> items) {
        dropItems(l, items.toArray(new ItemStack[0]));
    }

    public void dropItems(Location l, ItemStack... items) {
        for (ItemStack i : items) {
            if (LegacyFoundationAdapter.isAir(i.getType()))
                return;

            l.getWorld().dropItemNaturally(centerOfBlock(l), i);
        }
    }

    public Location centerOfBlock(Location location) {
        Location newLocation = location.clone();

        if (LegacyFoundationAdapter.isAtLeastMinecraft1_13()) {
            return newLocation;
        }

        return newLocation.add(0.5, 0.5, 0.5);
    }

    public void replaceDrop(List<ItemStack> drops, Material from, Material to) {
        for (int i = 0; i < drops.size(); i++) {
            ItemStack original = drops.get(i);
            if (original.getType() == from) {
                drops.set(i, original.withType(to));
            }
        }
    }

    public int getDropsAmount(List<ItemStack> drops, Material item) {
        return drops.stream().filter(itemStack -> itemStack.getType() == item)
                .mapToInt(itemStack -> itemStack.getAmount())
                .sum();
    }

    public void spawnOrb(Location l, int value) {
        spawnOrb(l, 1, value);
    }

    public void spawnOrb(Location l, int amount, int value) {
        if (amount <= 0 || value <= 0)
            return;

        Location location = LegacyFoundationAdapter.isAtLeastMinecraft1_13() ? l : l.clone().add(0.5, 0.5, 0.5);
        for (int i = 0; i < amount; i++) {
            ExperienceOrb orb = location.getWorld().spawn(location, ExperienceOrb.class);
            orb.setExperience(value);
        }
    }

    public int getBlockEXP(Material blockType) {
        String materialName = blockType.name();

        if (materialName.contains("REDSTONE_ORE"))
            return Extra.randomizar(1, 5);

        if (materialName.contains("COAL_ORE"))
            return Extra.randomizar(0, 2);
        if (materialName.contains("DIAMOND_ORE") || materialName.contains("EMERALD_ORE"))
            return Extra.randomizar(3, 7);
        if (materialName.contains("LAPIS_ORE") || materialName.contains("NETHER_QUARTZ_ORE"))
            return Extra.randomizar(2, 5);
        if (materialName.contains("IRON_ORE") || materialName.contains("GOLD_ORE"))
            return 1;

        return 0;
    }

    public boolean isOre(Material m) {
        return m.toString().contains("ORE");
    }

    public boolean isUHCWorld(World world) {
        return world.getName().contains(Settings.Game.UHC_WORLD_NAME);
    }
}
