package org.mcwonderland.uhc.storage;

import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mineacademy.fo.settings.YamlConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class SavedGameSettingsStore extends YamlConfig {

    public SavedGameSettingsStore() {
        loadConfiguration(UHCFiles.SAVED_GAMES);
    }

    public List<UHCGameSettings> load(UUID playerId) {
        String path = playerId.toString();

        if (!isSet(path))
            return new ArrayList<>();

        return getList(path, UHCGameSettings.class);
    }

    public void save(UUID playerId, Collection<UHCGameSettings> settings) {
        set(playerId.toString(), settings);
        save();
    }
}
