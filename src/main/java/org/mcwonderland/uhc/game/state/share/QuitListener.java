package org.mcwonderland.uhc.game.state.share;

import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public abstract class QuitListener implements Listener {

    @EventHandler
    public final void onQuit(PlayerQuitEvent e) {
        onPlayerQuit(e);
        e.setQuitMessage(LegacyFoundationAdapter.colorize(e.getQuitMessage()));
    }

    protected abstract void onPlayerQuit(PlayerQuitEvent e);

}
