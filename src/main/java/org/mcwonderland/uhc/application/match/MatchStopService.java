package org.mcwonderland.uhc.application.match;

import org.bukkit.Bukkit;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;

public final class MatchStopService {

    public void stopServer() {
        WorldLoadingCacheState.deleteCache();
        Bukkit.shutdown();
    }
}
