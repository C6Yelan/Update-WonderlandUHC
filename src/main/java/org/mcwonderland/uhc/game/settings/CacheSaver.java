package org.mcwonderland.uhc.game.settings;

import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.storage.WorldLoadingCache;
import org.mcwonderland.uhc.storage.WorldLoadingCacheStore;

/**
 * Legacy static facade kept for existing call sites.
 */
public final class CacheSaver {

    private static final WorldLoadingCacheStore store = new WorldLoadingCacheStore();
    private static UHCGameSettings settings;
    private static LoadingStatus loadingStatus;
    private static String host;
    private static MatchCenter matchCenter;

    static {
        loadCache(store.load());
    }

    private CacheSaver() {
    }

    public static UHCGameSettings getSettings() {
        return settings;
    }

    public static void setSettings(UHCGameSettings settings) {
        CacheSaver.settings = settings;
    }

    public static LoadingStatus getLoadingStatus() {
        return loadingStatus;
    }

    public static void setLoadingStatus(LoadingStatus loadingStatus) {
        CacheSaver.loadingStatus = loadingStatus;
    }

    public static String getHost() {
        return host;
    }

    public static void setHost(String host) {
        CacheSaver.host = host;
    }

    public static MatchCenter getMatchCenter() {
        return matchCenter;
    }

    public static void setMatchCenter(MatchCenter matchCenter) {
        CacheSaver.matchCenter = matchCenter;
    }

    public static void saveCache() {
        UHCGameSettings currentSettings = Game.getSettings();
        MatchCenter currentMatchCenter = Game.getGame().getMatchCenter();
        store.save(new WorldLoadingCache(loadingStatus, host, currentSettings, currentMatchCenter));
        matchCenter = currentMatchCenter;
    }

    public static void deleteCache() {
        store.delete();
    }

    private static void loadCache(WorldLoadingCache cache) {
        loadingStatus = cache.getLoadingStatus();
        host = cache.getHost();
        settings = cache.getSettings();
        matchCenter = cache.getMatchCenter();
    }
}
