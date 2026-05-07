package org.mcwonderland.uhc.storage;

import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;

public final class WorldLoadingCache {

    private final LoadingStatus loadingStatus;
    private final String host;
    private final UHCGameSettings settings;

    public WorldLoadingCache(LoadingStatus loadingStatus, String host, UHCGameSettings settings) {
        this.loadingStatus = loadingStatus == null ? LoadingStatus.CONFIGURING : loadingStatus;
        this.host = host == null ? "" : host;
        this.settings = settings;
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
}
