package org.mcwonderland.uhc.scenario.impl.rush;

import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioHastyBoys extends ConfigBasedScenario implements Listener {

    public ScenarioHastyBoys(ScenarioName name) {
        super(name);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        ItemStack result = e.getInventory().getResult();

        if (result == null)
            return;

        if (isTool(result.getType()))
            e.getInventory().setResult(applyHastyEnchants(result));
    }

    private ItemStack applyHastyEnchants(ItemStack result) {
        result.addUnsafeEnchantment(Enchantment.EFFICIENCY, 3);
        return result;
    }

    private boolean isTool(Material type) {
        String typeName = type.name();

        return typeName.contains("AXE")
                || typeName.contains("SHOVEL")
                || typeName.contains("PICKAXE")
                || typeName.contains("HOE");
    }

}
