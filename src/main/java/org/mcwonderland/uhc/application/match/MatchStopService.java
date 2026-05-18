package org.mcwonderland.uhc.application.match;

import org.bukkit.Bukkit;
import org.mcwonderland.uhc.game.settings.CacheSaver;

public final class MatchStopService {

    public void stopServer() {
        CacheSaver.deleteCache();
        Bukkit.shutdown();
    }
}
