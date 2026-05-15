package org.mcwonderland.uhc.scenario.impl.rush;

import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.core.rule.OreRuleSupport;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.WorldUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioCutClean extends ConfigBasedScenario implements Listener {

    public ScenarioCutClean(ScenarioName name) {
        super(name);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    protected void handleCutClean(UHCBlockBreakEvent e) {
        replaceOreDrops(e);

        if (shouldSetBlockExp(e.isDropsModified(), e.isExpToDropModified()))
            e.setExpToDrop(WorldUtils.getBlockEXP(e.getBlockType()));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        EntityType entityType = e.getEntityType();
        List<ItemStack> drops = e.getDrops();

        replaceEntityDrops(entityType, drops);
    }

    private void replaceOreDrops(UHCBlockBreakEvent e) {
        List<ItemStack> drops = e.getDrops();

        for (int i = 0; i < drops.size(); i++) {
            ItemStack drop = drops.get(i);
            Material cookedDrop = OreRuleSupport.cookedOreDrop(drop.getType());

            if (drop.getType() != cookedDrop)
                drops.set(i, drop.withType(cookedDrop));
        }
    }

    static boolean shouldSetBlockExp(boolean dropsModified, boolean expToDropModified) {
        return dropsModified && !expToDropModified;
    }

    private void replaceEntityDrops(EntityType entityType, List<ItemStack> drops) {
        switch (entityType) {
            case CHICKEN:
                WorldUtils.replaceDrop(drops, Material.CHICKEN, Material.COOKED_CHICKEN);
                break;
            case COW:
                WorldUtils.replaceDrop(drops, Material.BEEF, Material.COOKED_BEEF);
                break;
            case PIG:
                WorldUtils.replaceDrop(drops, Material.PORKCHOP, Material.COOKED_PORKCHOP);
                break;
            case SHEEP:
                WorldUtils.replaceDrop(drops, Material.MUTTON, Material.COOKED_MUTTON);
                break;
            case RABBIT:
                WorldUtils.replaceDrop(drops, Material.RABBIT, Material.COOKED_RABBIT);
                break;
            default:
                break;
        }
    }
}
