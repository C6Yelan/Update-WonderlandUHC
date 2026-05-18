package org.mcwonderland.uhc.game.settings.sub;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class UHCTimerSettings implements org.mcwonderland.uhc.api.game.UHCTimerSettings {
    private Integer pvpTime;
    private Integer damageTime;
    private Integer healTime;
    private Integer borderShrinkTime;
    private Integer disableNetherTime;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("Damage_Time", damageTime);
        map.put("Heal_Time", healTime);
        map.put("Pvp_Time", pvpTime);
        map.put("Border_Shrink_Time", borderShrinkTime);
        map.put("Disable_Nether_Time", disableNetherTime);

        return map;
    }

    public static UHCTimerSettings fromSection(ConfigurationSection section) {
        UHCTimerSettings timer = new UHCTimerSettings();

        timer.damageTime = integer(section, "Damage_Time", 60 * 5);
        timer.healTime = integer(section, "Heal_Time", 60 * 10);
        timer.pvpTime = integer(section, "Pvp_Time", 60 * 20);
        timer.borderShrinkTime = integer(section, "Border_Shrink_Time", 60 * 35);
        timer.disableNetherTime = integer(section, "Disable_Nether_Time", 60 * 40);

        return timer;
    }

    private static int integer(ConfigurationSection section, String path, int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }
}
