package org.mcwonderland.uhc.scenario.impl.block;

import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.model.VeinMiner;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.cuboid.SelectMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioTimber extends ConfigBasedScenario implements Listener {

    public ScenarioTimber(ScenarioName name) {
        super(name);
    }

    @EventHandler
    protected void onBlockBreak(UHCBlockBreakEvent e) {
        if (LegacyFoundationAdapter.isLog(e.getBlockType()))
            VeinMiner.mineVeins(e.getBlock(), e.getPlayer(), SelectMode.CUBE);
    }
}
