package org.mcwonderland.uhc.game.state.playing.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.game.state.share.MotdListener;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;

public class PlayingMotdListener extends MotdListener {

    @EventHandler
    public void onServerListPing(ServerListPingEvent e) {
        super.onServerListPing(e);
    }

    @Override
    protected String getMotd() {
        return PluginText.replaceToString(
                Messages.Motd.PLAYING,
                "{remaining}", UHCPlayers.countOf(uhcPlayer -> !uhcPlayer.isDead())
        );
    }
}
