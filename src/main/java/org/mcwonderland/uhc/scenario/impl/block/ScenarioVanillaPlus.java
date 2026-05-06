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
public class ScenarioVanillaPlus extends ConfigBasedScenario implements Listener {

    public ScenarioVanillaPlus(ScenarioName name) {
        super(name);
    }

    @EventHandler
    protected void onBlockBreak(UHCBlockBreakEvent e) {
        Material blockType = e.getBlockType();

        switch (blockType) {
            case GRAVEL:
                removeOriginalChance(e);
                replaceDropChance(e, 20, Material.GRAVEL, Material.FLINT);
                break;
        }
    }

    private void removeOriginalChance(UHCBlockBreakEvent e) {
        e.replaceDrop(Material.FLINT, Material.GRAVEL);
    }

    private void replaceDropChance(UHCBlockBreakEvent e, int percent, Material from, Material to) {
        if (LegacyFoundationAdapter.chance(percent))
            e.replaceDrop(from, to);
    }
}
