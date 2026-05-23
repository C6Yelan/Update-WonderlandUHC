package org.mcwonderland.uhc.scenario.impl.block;

import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.core.rule.OreRuleSupport;
import org.mcwonderland.uhc.platform.PlayerHand;
import org.mcwonderland.uhc.platform.random.PluginRandom;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.platform.sound.PluginSound;

import java.util.List;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioDoubleOrNothing extends ConfigBasedScenario implements Listener {

    @FilePath(name = "Nothing_Sound")
    private PluginSound nothingSound;
    @FilePath(name = "Trigger_Blocks")
    private List<Material> triggerBlocks;

    public ScenarioDoubleOrNothing(ScenarioName name) {
        super(name);
    }

    @EventHandler
    protected void onBlockBreak(UHCBlockBreakEvent e) {
        Material blockType = e.getBlockType();
        ItemStack tool = PlayerHand.getMainHandItem(e.getPlayer());

        if (shouldApply(blockType, tool, triggerBlocks))
            doubleOrNothing(e);
    }

    static boolean shouldApply(Material blockType, ItemStack tool, List<Material> triggerBlocks) {
        return shouldApply(blockType, hasSilkTouch(tool), triggerBlocks);
    }

    static boolean shouldApply(Material blockType, boolean hasSilkTouch, List<Material> triggerBlocks) {
        return OreRuleSupport.matchesAnyBlock(triggerBlocks, blockType) && !hasSilkTouch;
    }

    static boolean hasSilkTouch(ItemStack itemStack) {
        return itemStack != null && itemStack.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
    }

    private void doubleOrNothing(UHCBlockBreakEvent e) {
        if (PluginRandom.nextBoolean())
            e.getDrops().forEach(drop -> drop.setAmount(drop.getAmount() * 2));
        else {
            e.getDrops().clear();
            Extra.sound(e.getBlock().getLocation(), nothingSound);
        }
    }

    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLoreJoined(list, "{materials}", triggerBlocks, " - ");
    }
}
