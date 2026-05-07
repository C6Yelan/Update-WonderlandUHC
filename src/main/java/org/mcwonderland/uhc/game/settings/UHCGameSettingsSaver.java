package org.mcwonderland.uhc.game.settings;

import org.bukkit.entity.Player;
import org.mcwonderland.uhc.storage.SavedGameSettingsStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Legacy static facade kept for existing call sites.
 */
public final class UHCGameSettingsSaver {

    private static SavedGameSettingsStore store = new SavedGameSettingsStore();
    private static final Map<UUID, List<UHCGameSettings>> savedSettings = new HashMap<>();

    private UHCGameSettingsSaver() {
    }

    public static void reloadFromFile() {
        savedSettings.clear();
        store = new SavedGameSettingsStore();
    }

    public static List<UHCGameSettings> getSavedSettings(Player player) {
        UUID playerId = player.getUniqueId();
        List<UHCGameSettings> settings = savedSettings.get(playerId);

        if (settings == null)
            settings = loadPlayerSavedGames(playerId);

        return settings;
    }

    public static void saveGameSettings(Player player) {
        UUID playerId = player.getUniqueId();
        List<UHCGameSettings> settings = savedSettings.get(playerId);

        if (settings != null)
            store.save(playerId, settings);
    }

    private static List<UHCGameSettings> loadPlayerSavedGames(UUID playerId) {
        List<UHCGameSettings> settings = store.load(playerId);

        savedSettings.put(playerId, settings);

        return settings;
    }
}
