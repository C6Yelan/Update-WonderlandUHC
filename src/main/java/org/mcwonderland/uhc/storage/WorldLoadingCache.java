package org.mcwonderland.uhc.storage;

import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;

public final class WorldLoadingCache {

    private final LoadingStatus loadingStatus;
    private final String host;
    private final UHCGameSettings settings;
    private final MatchCenter matchCenter;

    public WorldLoadingCache(LoadingStatus loadingStatus, String host, UHCGameSettings settings) {
        this(loadingStatus, host, settings, null);
    }

    public WorldLoadingCache(LoadingStatus loadingStatus, String host, UHCGameSettings settings, MatchCenter matchCenter) {
        this.loadingStatus = loadingStatus == null ? LoadingStatus.CONFIGURING : loadingStatus;
        this.host = host == null ? "" : host;
        this.settings = settings;
        this.matchCenter = matchCenter;
    }

    public LoadingStatus getLoadingStatus() {
        return loadingStatus;
    }

    public String getHost() {
        return host;
    }

    public UHCGameSettings getSettings() {
        return settings;
    }

    public MatchCenter getMatchCenter() {
        return matchCenter;
    }
}
