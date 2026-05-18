package org.mcwonderland.uhc.scenario.impl.consume;

import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.Collection;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioPotionLess extends ConfigBasedScenario implements Listener {

    public ScenarioPotionLess(ScenarioName name) {
        super(name);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrinkPotion(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == PluginMaterials.materialOf("POTION"))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrew(BrewEvent e) {
        e.setCancelled(true);
    }

    @Override
    protected Collection<Listener> initListeners() {
        Collection<Listener> listeners = super.initListeners();
        listeners.add(new PotionEffectListener());
        listeners.add(new LingeringPotionListener());

        return listeners;
    }

    class LingeringPotionListener implements Listener {
        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onLingeringPotionSplash(org.bukkit.event.entity.LingeringPotionSplashEvent e) {
            e.setCancelled(true);
        }
    }

    class PotionEffectListener implements Listener {
        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void onEntityPotionEffect(org.bukkit.event.entity.EntityPotionEffectEvent e) {
            if (e.getCause().toString().contains("POTION"))
                e.setCancelled(true);
        }
    }
}
