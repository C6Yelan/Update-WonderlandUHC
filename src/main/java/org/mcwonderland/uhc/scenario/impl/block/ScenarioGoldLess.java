package org.mcwonderland.uhc.scenario.impl.block;

import org.mcwonderland.uhc.core.rule.OreRuleSupport;
import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioGoldLess extends ConfigBasedScenario implements Listener {

    public ScenarioGoldLess(ScenarioName name) {
        super(name);
    }

    @EventHandler
    protected void onBlockBreak(UHCBlockBreakEvent e) {
        if (OreRuleSupport.isGoldOre(e.getBlockType()))
            e.getDrops().removeIf(drop -> OreRuleSupport.isGoldDrop(drop.getType()));
    }
}
