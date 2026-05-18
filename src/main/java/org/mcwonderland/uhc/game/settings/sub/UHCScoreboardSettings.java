package org.mcwonderland.uhc.game.settings.sub;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.mcwonderland.uhc.scoreboard.SidebarTheme;
import org.mineacademy.fo.remain.CompChatColor;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class UHCScoreboardSettings {
    private SidebarTheme sidebarTheme;
    private Integer scoreboardUpdateTick;
    private CompChatColor heartColor;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("Theme", sidebarTheme.getName());
        map.put("Update_Tick", scoreboardUpdateTick);
        map.put("Heart_Color", heartColor.toSaveableString());

        return map;
    }

    public static UHCScoreboardSettings fromSection(ConfigurationSection section) {
        UHCScoreboardSettings settings = new UHCScoreboardSettings();

        settings.sidebarTheme = SidebarTheme.getThemeOrDefault(section == null ? "" : section.getString("Theme", ""));
        settings.scoreboardUpdateTick = section == null ? 5 : section.getInt("Update_Tick", 5);
        settings.heartColor = CompChatColor.of("&c");//map.getString("Heart_Color", "&c"));

        return settings;
    }
}
