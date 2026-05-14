package org.mcwonderland.uhc.application.match;

import org.bukkit.Bukkit;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.util.Extra;

public final class MatchStopService {

    public void stopServer() {
        CacheSaver.deleteCache();
        LegacyFoundationAdapter.getOnlinePlayers().forEach(Extra::sendToFallbackServer);
        Bukkit.shutdown();
    }
}
