package org.mcwonderland.uhc.practice;

import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.mcwonderland.uhc.util.PlayerUtils;

public class CommonPracticeListener implements Listener {

    private Practice practice;

    public CommonPracticeListener(Practice practice) {
        this.practice = practice;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();

        if (practice.isInPractice(player))
            e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        Player killer = player.getKiller();

        if (practice.isInPractice(player)) {
            e.getDrops().clear();

            respawnAndRestuff(player);

            if (killer != null)
                killer.setHealth(getMaxHealth(killer));
        }
    }

    private void respawnAndRestuff(Player player) {
        PluginScheduler.runLater(1, () -> {
            if (!player.isOnline() || !practice.isInPractice(player))
                return;

            PlayerUtils.respawnIfDead(player);

            PluginScheduler.runLater(1, () -> {
                if (!player.isOnline() || !practice.isInPractice(player) || player.isDead())
                    return;

                player.setHealth(getMaxHealth(player));
                practice.stuff(player);
            });
        });
    }

    private double getMaxHealth(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 20.0 : maxHealth.getValue();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        if (practice.isInPractice(player))
            practice.quit(player);
    }

}
