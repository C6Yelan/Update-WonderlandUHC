package org.mcwonderland.uhc.game.state.playing.listener;

import org.mcwonderland.uhc.game.CombatRelog;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class CombatRelogListener implements Listener {

    @EventHandler
    public void onCombatRelogEntityTeleport(EntityTeleportEvent e) {
        if (CombatRelog.isRelogEntity(e.getEntity())) {
            LegacyFoundationAdapter.setChunkForceLoaded(e.getFrom().getChunk(), false);
            LegacyFoundationAdapter.setChunkForceLoaded(e.getTo().getChunk(), true);
        }
    }

    @EventHandler
    public void disableTradeWithCombatLogger(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();

        if (CombatRelog.isRelogEntity(entity))
            e.setCancelled(true);
    }
}
