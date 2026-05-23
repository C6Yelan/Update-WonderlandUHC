package org.mcwonderland.uhc.listener;

import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BooleanEvents implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UHCPlayer player = UHCPlayer.getUHCPlayer(e.getPlayer());

        

        PluginScheduler.runLater(1, () -> {
            if (player.isOnline())
                player.checkHide();
        });
    }
}
