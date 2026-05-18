package org.mcwonderland.uhc.application.match;

import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.bukkit.Bukkit;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.util.Extra;

public final class MatchStopService {

    public void stopServer() {
        CacheSaver.deleteCache();
        PluginPlayers.onlinePlayers().forEach(Extra::sendToFallbackServer);
        Bukkit.shutdown();
    }
}
