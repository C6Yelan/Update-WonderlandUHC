package org.mcwonderland.uhc.scenario.impl.block;

import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.core.rule.OreRuleSupport;
import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.model.VeinMiner;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.cuboid.SelectMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioBloodDiamonds extends ConfigBasedScenario implements Listener {

    @FilePath(name = "Damage")
    private Integer damage;

    public ScenarioBloodDiamonds(ScenarioName name) {
        super(name);
    }

    @EventHandler
    public void onBlockBreak(UHCBlockBreakEvent e) {
        if (!OreRuleSupport.isDiamondOre(e.getBlockType()) || VeinMiner.isMining(e.getPlayer()))
            return;

        e.getPlayer().damage(calculateDamageAmount(damage, getAffectedDiamondCount(e)));
    }

    private int getAffectedDiamondCount(UHCBlockBreakEvent e) {
        if (!isVeinMinerEnabled() || !e.getPlayer().isSneaking())
            return 1;

        return 1 + VeinMiner.countConnectedBlocks(e.getBlock(), SelectMode.CONNECT);
    }

    private boolean isVeinMinerEnabled() {
        Scenario veinMiner = WonderlandUHC.getInstance().getScenarioManager().getScenario(ScenarioName.VEIN_MINERS);
        return veinMiner != null && veinMiner.isEnabled();
    }

    static double calculateDamageAmount(Integer damage, int diamondOreCount) {
        return Math.max(0, damage == null ? 1 : damage) * Math.max(1, diamondOreCount);
    }

    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLore(list, "{heal}", damage);
    }
}
