package org.mcwonderland.uhc.game.settings.sub;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.mcwonderland.uhc.api.enums.TeamSplitMode;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class UHCTeamSettings implements org.mcwonderland.uhc.api.game.UHCTeamSettings {
    private Integer teamSize;
    private Boolean allowTeamFire;
    private TeamSplitMode teamSplitMode;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("Team_Size", teamSize);
        map.put("Allow_Team_Fire", allowTeamFire);
        map.put("Team_Split_Mode", teamSplitMode);

        return map;
    }

    public static UHCTeamSettings fromSection(ConfigurationSection section) {
        UHCTeamSettings uhcTeamSettings = new UHCTeamSettings();

        uhcTeamSettings.teamSize = section == null ? 1 : section.getInt("Team_Size", 1);
        uhcTeamSettings.allowTeamFire = section != null && section.getBoolean("Allow_Team_Fire", false);
        uhcTeamSettings.teamSplitMode = teamSplitMode(section);

        return uhcTeamSettings;
    }

    @Override
    public Boolean isAllowTeamFire() {
        return getAllowTeamFire();
    }

    private static TeamSplitMode teamSplitMode(ConfigurationSection section) {
        String name = section == null ? null : section.getString("Team_Split_Mode");

        if (name == null || name.isBlank())
            return TeamSplitMode.CHOSEN;

        try {
            return TeamSplitMode.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return TeamSplitMode.CHOSEN;
        }
    }
}
