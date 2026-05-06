package org.mcwonderland.uhc.listener;

import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class StatsListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UHCPlayer uhcPlayer = UHCPlayer.getUHCPlayer(e.getPlayer());

        LegacyFoundationAdapter.runLaterAsync(() -> WonderlandUHC.getStatsStorage().save(uhcPlayer));
    }


}
