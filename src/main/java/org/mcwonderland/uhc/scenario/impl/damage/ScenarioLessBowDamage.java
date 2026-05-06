package org.mcwonderland.uhc.scenario.impl.damage;

import org.mcwonderland.uhc.api.event.player.UHCPlayerDamageByEntityEvent;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * 2019-12-06 上午 10:23
 */
public class ScenarioLessBowDamage extends ConfigBasedScenario implements Listener {

    @FilePath(name = "Decrease_Percent")
    private Integer decreasePercent;

    public ScenarioLessBowDamage(ScenarioName name) {
        super(name);
    }

    @EventHandler
    public void onGamingEntityDamage(UHCPlayerDamageByEntityEvent e) {
        Player arrowShooter = PlayerUtils.getShooter(e.getDamager());

        if (arrowShooter == null)
            return;

        e.setDamage(e.getDamage() * (decreasePercent / 100D));
    }


    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLore(list, "{percent}", decreasePercent);
    }
}
