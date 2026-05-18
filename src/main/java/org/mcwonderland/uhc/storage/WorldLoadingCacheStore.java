package org.mcwonderland.uhc.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.settings.UHCFiles;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class WorldLoadingCacheStore {
    private static final String MATCH_CENTER_X = "Match_Center.X";
    private static final String MATCH_CENTER_Z = "Match_Center.Z";
    private static final String MATCH_CENTER_BORDER_SIZE = "Match_Center.Border_Size";
    private final File file;
    private final YamlConfiguration configuration;

    public WorldLoadingCacheStore() {
        this.file = new File(WonderlandUHC.getInstance().getDataFolder(), UHCFiles.CACHE);
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public WorldLoadingCache load() {
        LoadingStatus loadingStatus = loadingStatus();
        String host = configuration.getString("Host", "");
        UHCGameSettings settings = loadSettings();
        MatchCenter matchCenter = loadMatchCenter();

        return new WorldLoadingCache(loadingStatus, host, settings, matchCenter);
    }

    public void save(WorldLoadingCache cache) {
        configuration.set("Host", cache.getHost());
        configuration.set("Loading_Status", cache.getLoadingStatus().name());
        configuration.set("Settings", cache.getSettings() == null ? null : cache.getSettings().toMap());
        saveMatchCenter(cache.getMatchCenter());
        saveFile();
    }

    private LoadingStatus loadingStatus() {
        String status = configuration.getString("Loading_Status");

        if (status == null || status.isBlank())
            return LoadingStatus.CONFIGURING;

        try {
            return LoadingStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return LoadingStatus.CONFIGURING;
        }
    }

    private UHCGameSettings loadSettings() {
        ConfigurationSection section = configuration.getConfigurationSection("Settings");

        return section == null ? null : UHCGameSettings.fromSection(section);
    }

    private MatchCenter loadMatchCenter() {
        int borderSize = configuration.getInt(MATCH_CENTER_BORDER_SIZE, 0);

        if (borderSize <= 0)
            return null;

        return new MatchCenter(
                configuration.getInt(MATCH_CENTER_X, 0),
                configuration.getInt(MATCH_CENTER_Z, 0),
                borderSize
        );
    }

    private void saveMatchCenter(MatchCenter matchCenter) {
        if (matchCenter == null) {
            configuration.set(MATCH_CENTER_X, null);
            configuration.set(MATCH_CENTER_Z, null);
            configuration.set(MATCH_CENTER_BORDER_SIZE, null);
            return;
        }

        configuration.set(MATCH_CENTER_X, matchCenter.getX());
        configuration.set(MATCH_CENTER_Z, matchCenter.getZ());
        configuration.set(MATCH_CENTER_BORDER_SIZE, matchCenter.getBorderSize());
    }

    public void delete() {
        if (file.exists())
            file.delete();
    }

    private void saveFile() {
        try {
            configuration.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save " + UHCFiles.CACHE, ex);
        }
    }
}
