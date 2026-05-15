package org.mcwonderland.uhc.game.state.playing.listener;

import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.util.GameUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ItemListener implements Listener {

    @EventHandler
    public void onPlayerPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player))
            return;

        if (!GameUtils.isGamingPlayer(player))
            e.setCancelled(true);
    }


    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        UHCPlayer uhcPlayer = UHCPlayer.getUHCPlayer(e.getPlayer());

        if (uhcPlayer.isDead())
            e.setCancelled(true);
    }
}
