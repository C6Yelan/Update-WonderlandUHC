package org.mcwonderland.uhc.game.settings.sub;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.mcwonderland.uhc.platform.text.PluginColor;
import org.mcwonderland.uhc.scoreboard.SidebarTheme;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class UHCScoreboardSettings {
    private static final PluginColor DEFAULT_HEART_COLOR = PluginColor.RED;

    private SidebarTheme sidebarTheme;
    private Integer scoreboardUpdateTick;
    private PluginColor heartColor;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("Theme", sidebarTheme.getName());
        map.put("Update_Tick", scoreboardUpdateTick);
        map.put("Heart_Color", heartColor.name().toLowerCase(Locale.ROOT));

        return map;
    }

    public static UHCScoreboardSettings fromSection(ConfigurationSection section) {
        UHCScoreboardSettings settings = new UHCScoreboardSettings();

        settings.sidebarTheme = SidebarTheme.getThemeOrDefault(section == null ? "" : section.getString("Theme", ""));
        settings.scoreboardUpdateTick = section == null ? 5 : section.getInt("Update_Tick", 5);
        settings.heartColor = parseHeartColor(section == null ? "" : section.getString("Heart_Color", ""));

        return settings;
    }

    private static PluginColor parseHeartColor(String value) {
        return PluginColor.parseOrDefault(value, DEFAULT_HEART_COLOR);
    }
}
