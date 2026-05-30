package org.mcwonderland.uhc.game.state.playing.listener;

import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import net.kyori.adventure.text.Component;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.PotionApplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GoldenHeadListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEatGoldenHead(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        Player player = e.getPlayer();
        Component name = getItemName(item);

        if (name.equals(goldenHeadName())) {
            PluginScheduler.runLater(1, () -> {
                if (!player.isOnline() || player.isDead())
                    return;

                PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1);

                PotionApplier.addPotionEffect(player, regen);

                if (!isScenarioEnabled(ScenarioName.ABSORPTION_LESS)) {
                    PotionEffect absorp = new PotionEffect(PotionEffectType.ABSORPTION, 60 * 20 * 2, 0);
                    PotionApplier.addPotionEffect(player, absorp);
                }
            });
        }
    }

    private Component getItemName(ItemStack item) {
        if (item == null || item.getItemMeta() == null)
            return Component.empty();

        ItemMeta meta = item.getItemMeta();
        Component displayName = meta.displayName();
        return displayName == null ? Component.empty() : displayName;
    }

    private Component goldenHeadName() {
        return PluginText.toItemComponent(Settings.Misc.GOLDEN_HEAD_NAME);
    }

    private boolean isScenarioEnabled(ScenarioName scenarioName) {
        Scenario scenario = WonderlandUHC.getInstance().getScenarioManager().getScenario(scenarioName);
        return scenario != null && scenario.isEnabled();
    }
}
