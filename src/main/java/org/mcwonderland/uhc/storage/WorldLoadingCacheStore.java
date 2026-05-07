package org.mcwonderland.uhc.storage;

import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mineacademy.fo.settings.YamlConfig;

import java.io.File;

public final class WorldLoadingCacheStore extends YamlConfig {

    public WorldLoadingCacheStore() {
        loadConfiguration(null, UHCFiles.CACHE);
    }

    public WorldLoadingCache load() {
        LoadingStatus loadingStatus = get("Loading_Status", LoadingStatus.class, LoadingStatus.CONFIGURING);
        String host = getString("Host", "");
        UHCGameSettings settings = get("Settings", UHCGameSettings.class);

        return new WorldLoadingCache(loadingStatus, host, settings);
    }

    public void save(WorldLoadingCache cache) {
        set("Host", cache.getHost());
        set("Loading_Status", cache.getLoadingStatus());
        set("Settings", cache.getSettings());
        save();
    }

    public void delete() {
        File file = LegacyFoundationAdapter.getFile(getFileName());

        if (file.exists()) {
            YamlConfig.clearLoadedSections();
            file.delete();
        }
    }
}
