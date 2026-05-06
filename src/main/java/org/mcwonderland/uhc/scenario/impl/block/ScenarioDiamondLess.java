package org.mcwonderland.uhc.scenario.impl.block;

import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioDiamondLess extends ConfigBasedScenario implements Listener {

    public ScenarioDiamondLess(ScenarioName name) {
        super(name);
    }

    @EventHandler
    public void onBlockBreak(UHCBlockBreakEvent e) {
        if (e.getBlockType() == LegacyFoundationAdapter.materialOf("DIAMOND_ORE"))
            e.removeDrop(Material.DIAMOND, Material.DIAMOND_ORE);
    }
}
