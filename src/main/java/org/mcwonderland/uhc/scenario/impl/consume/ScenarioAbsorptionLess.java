package org.mcwonderland.uhc.scenario.impl.consume;

import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * 2019-12-07 下午 02:33
 */
public class ScenarioAbsorptionLess extends ConfigBasedScenario implements Listener {

    public ScenarioAbsorptionLess(ScenarioName name) {
        super(name);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();

        PluginScheduler.runLater(0, () -> player.removePotionEffect(PotionEffectType.ABSORPTION));
    }
}
