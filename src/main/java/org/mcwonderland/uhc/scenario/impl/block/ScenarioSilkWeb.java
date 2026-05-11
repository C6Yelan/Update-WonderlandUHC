package org.mcwonderland.uhc.scenario.impl.block;

import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.platform.PlayerHand;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioSilkWeb extends ConfigBasedScenario implements Listener {

    public ScenarioSilkWeb(ScenarioName name) {
        super(name);
    }

    @EventHandler
    public void onBreakCobweb(UHCBlockBreakEvent e) {
        Material blockType = e.getBlockType();

        if (blockType == LegacyFoundationAdapter.materialOf("COBWEB")) {
            if (usingShears(e.getPlayer()))
                e.replaceDrop(Material.STRING, Material.COBWEB);
            else {
                e.replaceDrop(Material.COBWEB, Material.STRING);
                e.setHandleCustom(true);
            }
        }
    }

    private boolean usingShears(Player player) {
        return PlayerHand.getMainHandItem(player).getType() == LegacyFoundationAdapter.materialOf("SHEARS");
    }
}
