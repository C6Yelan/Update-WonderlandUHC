package org.mcwonderland.uhc.game.settings.sub;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.mcwonderland.uhc.game.border.BorderType;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@Setter
public class UHCBorderSettings implements org.mcwonderland.uhc.api.game.UHCBorderSettings {
    private Integer initialBorder;
    private Integer initialNetherBorder;
    private Integer finalSizeOfShrinkModeBorder;
    private BorderType borderType;
    private Double borderShrinkSpeed;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("Initial_Border", initialBorder);
        map.put("Initial_Nether_Border", initialNetherBorder);
        map.put("Final_Size_Of_Shrink_Mode_Border", finalSizeOfShrinkModeBorder);
        map.put("Border_Type", borderType == null ? null : borderType.name());
        map.put("Border_Shrink_Speed", borderShrinkSpeed);

        return map;
    }

    public static UHCBorderSettings fromSection(ConfigurationSection section) {
        UHCBorderSettings uhcBorderSettings = new UHCBorderSettings();

        uhcBorderSettings.initialBorder = integer(section, "Initial_Border", 2000);
        uhcBorderSettings.initialNetherBorder = integer(section, "Initial_Nether_Border", 1000);
        uhcBorderSettings.finalSizeOfShrinkModeBorder = integer(section, "Final_Size_Of_Shrink_Mode_Border", 25);
        uhcBorderSettings.borderType = borderType(section);
        uhcBorderSettings.borderShrinkSpeed = section == null ? 0.1 : section.getDouble("Border_Shrink_Speed", 0.1);

        return uhcBorderSettings;
    }

    private static int integer(ConfigurationSection section, String path, int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }

    private static BorderType borderType(ConfigurationSection section) {
        String name = section == null ? null : section.getString("Border_Type");

        if (name == null || name.isBlank())
            return BorderType.TIMER;

        try {
            return BorderType.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BorderType.TIMER;
        }
    }
}
