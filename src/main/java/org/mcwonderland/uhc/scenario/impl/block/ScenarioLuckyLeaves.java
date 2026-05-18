package org.mcwonderland.uhc.scenario.impl.block;

import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.platform.random.PluginRandom;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.WorldUtils;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

import java.util.List;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioLuckyLeaves extends ConfigBasedScenario implements Listener {

    @FilePath(name = "Golden_Apple_Percent")
    private Integer goldenApplePercent;

    public ScenarioLuckyLeaves(ScenarioName name) {
        super(name);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeaveDecay(LeavesDecayEvent e) {
        randomDropGoldenApple(e.getBlock());
    }

    @EventHandler
    protected void onBlockBreak(UHCBlockBreakEvent e) {
        if (PluginMaterials.isLeaves(e.getBlockType()))
            randomDropGoldenApple(e.getBlock());
    }

    private void randomDropGoldenApple(Block block) {
        if (PluginRandom.chance(goldenApplePercent))
            WorldUtils.dropItems(block.getLocation(), PluginMaterials.itemOf("GOLDEN_APPLE"));
    }

    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLore(list, "{percent}", goldenApplePercent);
    }
}
