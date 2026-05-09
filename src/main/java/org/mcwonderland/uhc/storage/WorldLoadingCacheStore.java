package org.mcwonderland.uhc.storage;

import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mineacademy.fo.settings.YamlConfig;

import java.io.File;

public final class WorldLoadingCacheStore extends YamlConfig {
    private static final String MATCH_CENTER_X = "Match_Center.X";
    private static final String MATCH_CENTER_Z = "Match_Center.Z";
    private static final String MATCH_CENTER_BORDER_SIZE = "Match_Center.Border_Size";

    public WorldLoadingCacheStore() {
        loadConfiguration(null, UHCFiles.CACHE);
    }

    public WorldLoadingCache load() {
        LoadingStatus loadingStatus = get("Loading_Status", LoadingStatus.class, LoadingStatus.CONFIGURING);
        String host = getString("Host", "");
        UHCGameSettings settings = get("Settings", UHCGameSettings.class);
        MatchCenter matchCenter = loadMatchCenter();

        return new WorldLoadingCache(loadingStatus, host, settings, matchCenter);
    }

    public void save(WorldLoadingCache cache) {
        set("Host", cache.getHost());
        set("Loading_Status", cache.getLoadingStatus());
        set("Settings", cache.getSettings());
        saveMatchCenter(cache.getMatchCenter());
        save();
    }

    private MatchCenter loadMatchCenter() {
        int borderSize = get(MATCH_CENTER_BORDER_SIZE, Integer.class, 0);

        if (borderSize <= 0)
            return null;

        return new MatchCenter(
                get(MATCH_CENTER_X, Integer.class, 0),
                get(MATCH_CENTER_Z, Integer.class, 0),
                borderSize
        );
    }

    private void saveMatchCenter(MatchCenter matchCenter) {
        if (matchCenter == null)
            return;

        set(MATCH_CENTER_X, matchCenter.getX());
        set(MATCH_CENTER_Z, matchCenter.getZ());
        set(MATCH_CENTER_BORDER_SIZE, matchCenter.getBorderSize());
    }

    public void delete() {
        File file = LegacyFoundationAdapter.getFile(getFileName());

        if (file.exists()) {
            YamlConfig.clearLoadedSections();
            file.delete();
        }
    }
}
