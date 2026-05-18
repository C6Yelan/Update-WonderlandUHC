package org.mcwonderland.uhc.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.settings.UHCFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SavedGameSettingsStore {

    private final File file;
    private final YamlConfiguration configuration;

    public SavedGameSettingsStore() {
        this.file = new File(WonderlandUHC.getInstance().getDataFolder(), UHCFiles.SAVED_GAMES);
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public List<UHCGameSettings> load(UUID playerId) {
        String path = playerId.toString();

        if (!configuration.isSet(path))
            return new ArrayList<>();

        List<UHCGameSettings> loadedSettings = new ArrayList<>();

        for (Map<?, ?> map : configuration.getMapList(path))
            loadedSettings.add(UHCGameSettings.fromMap(map));

        return loadedSettings;
    }

    public void save(UUID playerId, Collection<UHCGameSettings> settings) {
        List<Map<String, Object>> values = new ArrayList<>();

        for (UHCGameSettings setting : settings)
            values.add(setting.toMap());

        configuration.set(playerId.toString(), values);
        saveFile();
    }

    private void saveFile() {
        try {
            configuration.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save " + UHCFiles.SAVED_GAMES, ex);
        }
    }
}
