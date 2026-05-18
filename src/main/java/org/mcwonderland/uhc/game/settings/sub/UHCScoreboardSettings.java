package org.mcwonderland.uhc.game.settings.sub;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.mcwonderland.uhc.scoreboard.SidebarTheme;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class UHCScoreboardSettings {
    private static final ChatColor DEFAULT_HEART_COLOR = ChatColor.RED;

    private SidebarTheme sidebarTheme;
    private Integer scoreboardUpdateTick;
    private ChatColor heartColor;

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

    private static ChatColor parseHeartColor(String value) {
        if (value == null || value.isBlank())
            return DEFAULT_HEART_COLOR;

        String normalized = value.trim();

        if (normalized.length() == 2 && (normalized.charAt(0) == '&' || normalized.charAt(0) == ChatColor.COLOR_CHAR))
            return parseLegacyColor(normalized.charAt(1));

        try {
            ChatColor color = ChatColor.valueOf(normalized.toUpperCase(Locale.ROOT));
            return color.isColor() ? color : DEFAULT_HEART_COLOR;
        } catch (IllegalArgumentException ex) {
            return DEFAULT_HEART_COLOR;
        }
    }

    private static ChatColor parseLegacyColor(char code) {
        char normalizedCode = Character.toLowerCase(code);

        for (ChatColor color : ChatColor.values())
            if (color.isColor() && color.toString().charAt(1) == normalizedCode)
                return color;

        return DEFAULT_HEART_COLOR;
    }
}
