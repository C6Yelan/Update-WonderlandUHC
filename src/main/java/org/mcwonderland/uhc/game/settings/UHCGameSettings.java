package org.mcwonderland.uhc.game.settings;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mcwonderland.uhc.game.settings.sub.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
public class UHCGameSettings implements Cloneable, org.mcwonderland.uhc.api.game.UHCGameSettings {

    private String title;
    private int maxPlayers;
    private int appleRate;
    private int initialExperience;
    private boolean whitelistOn;
    private boolean usingNether;
    private boolean enderPearlDamage;
    private String generator;
    private UHCTimerSettings timer;
    private UHCTeamSettings teamSettings;
    private UHCBorderSettings borderSettings;
    private UHCScoreboardSettings scoreboardSettings;
    private UHCItemSettings itemSettings;
    private Set<String> scenarios;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("Title", title);
        map.put("Max_Players", maxPlayers);
        map.put("Apple_Rate", appleRate);
        map.put("Initial_Experience", initialExperience);
        map.put("Whitelist_On", whitelistOn);
        map.put("Using_Nether", usingNether);
        map.put("Ender_Pearl_Damage", enderPearlDamage);

        map.put("Generator", generator);
        map.put("Timer", timer.toMap());
        map.put("Team_Settings", teamSettings.toMap());
        map.put("Border_Settings", borderSettings.toMap());
        map.put("Scoreboard_Settings", scoreboardSettings.toMap());
        map.put("Item_Settings", itemSettings.toMap());
        map.put("Scenarios", new ArrayList<>(scenarios));

        return map;
    }

    public static UHCGameSettings fromSection(ConfigurationSection section) {
        UHCGameSettings settings = new UHCGameSettings();

        settings.title = string(section, "Title", "&a&lWonderland&f&lUHC");
        settings.maxPlayers = integer(section, "Max_Players", 100);
        settings.appleRate = integer(section, "Apple_Rate", 2);
        settings.initialExperience = integer(section, "Initial_Experience", 1);
        settings.whitelistOn = bool(section, "Whitelist_On", true);
        settings.usingNether = bool(section, "Using_Nether", false);
        settings.enderPearlDamage = bool(section, "Ender_Pearl_Damage", false);
        settings.generator = string(section, "Generator", "");
        settings.scenarios = new HashSet<>(section == null ? List.of() : section.getStringList("Scenarios"));

        settings.timer = UHCTimerSettings.fromSection(section(section, "Timer"));
        settings.teamSettings = UHCTeamSettings.fromSection(section(section, "Team_Settings"));
        settings.borderSettings = UHCBorderSettings.fromSection(section(section, "Border_Settings"));
        settings.scoreboardSettings = UHCScoreboardSettings.fromSection(section(section, "Scoreboard_Settings"));
        settings.itemSettings = UHCItemSettings.fromSection(section(section, "Item_Settings"));

        return settings;
    }

    public static UHCGameSettings fromMap(Map<?, ?> map) {
        if (map == null || map.isEmpty())
            return defaultSettings();

        YamlConfiguration configuration = new YamlConfiguration();
        copyMap(configuration, map);

        return fromSection(configuration);
    }

    public static UHCGameSettings defaultSettings() {
        return UHCGameSettings.fromSection(null);
    }

    @Override
    public UHCGameSettings clone() {
        return UHCGameSettings.fromMap(toMap());
    }

    private static String string(ConfigurationSection section, String path, String fallback) {
        return section == null ? fallback : section.getString(path, fallback);
    }

    private static int integer(ConfigurationSection section, String path, int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }

    private static boolean bool(ConfigurationSection section, String path, boolean fallback) {
        return section == null ? fallback : section.getBoolean(path, fallback);
    }

    private static ConfigurationSection section(ConfigurationSection root, String path) {
        return root == null ? null : root.getConfigurationSection(path);
    }

    private static void copyMap(ConfigurationSection section, Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection nestedSection)
                copyMap(section.createSection(key), nestedSection.getValues(false));
            else if (value instanceof Map<?, ?> nestedMap && !isConfigurationSerializableMap(nestedMap))
                copyMap(section.createSection(key), nestedMap);
            else
                section.set(key, value);
        }
    }

    private static boolean isConfigurationSerializableMap(Map<?, ?> map) {
        return map.containsKey("==");
    }
}
