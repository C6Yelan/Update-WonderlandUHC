package org.mcwonderland.uhc.platform.text;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

public enum PluginColor {
    BLACK('0', NamedTextColor.BLACK, Material.BLACK_WOOL),
    DARK_BLUE('1', NamedTextColor.DARK_BLUE, Material.BLUE_WOOL),
    DARK_GREEN('2', NamedTextColor.DARK_GREEN, Material.GREEN_WOOL),
    DARK_AQUA('3', NamedTextColor.DARK_AQUA, Material.CYAN_WOOL),
    DARK_RED('4', NamedTextColor.DARK_RED, Material.RED_WOOL),
    DARK_PURPLE('5', NamedTextColor.DARK_PURPLE, Material.PURPLE_WOOL),
    GOLD('6', NamedTextColor.GOLD, Material.ORANGE_WOOL),
    GRAY('7', NamedTextColor.GRAY, Material.LIGHT_GRAY_WOOL),
    DARK_GRAY('8', NamedTextColor.DARK_GRAY, Material.GRAY_WOOL),
    BLUE('9', NamedTextColor.BLUE, Material.BLUE_WOOL),
    GREEN('a', NamedTextColor.GREEN, Material.LIME_WOOL),
    AQUA('b', NamedTextColor.AQUA, Material.LIGHT_BLUE_WOOL),
    RED('c', NamedTextColor.RED, Material.RED_WOOL),
    LIGHT_PURPLE('d', NamedTextColor.LIGHT_PURPLE, Material.MAGENTA_WOOL),
    YELLOW('e', NamedTextColor.YELLOW, Material.YELLOW_WOOL),
    WHITE('f', NamedTextColor.WHITE, Material.WHITE_WOOL);

    public static final List<PluginColor> SELECTABLE = List.of(values());

    private static final char LEGACY_COLOR_CHAR = '\u00A7';

    private final char legacyCode;
    private final NamedTextColor textColor;
    private final Material woolMaterial;

    PluginColor(char legacyCode, NamedTextColor textColor, Material woolMaterial) {
        this.legacyCode = legacyCode;
        this.textColor = textColor;
        this.woolMaterial = woolMaterial;
    }

    public static PluginColor parseOrDefault(String value, PluginColor fallback) {
        if (value == null || value.isBlank())
            return fallback;

        String normalized = value.trim();

        if (normalized.length() == 2 && (normalized.charAt(0) == '&' || normalized.charAt(0) == LEGACY_COLOR_CHAR))
            return fromLegacyCodeOrDefault(normalized.charAt(1), fallback);

        try {
            return valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public NamedTextColor toNamedTextColor() {
        return textColor;
    }

    public Material toWoolMaterial() {
        return woolMaterial;
    }

    public String legacyColor() {
        return String.valueOf(LEGACY_COLOR_CHAR) + legacyCode;
    }

    @Override
    public String toString() {
        return legacyColor();
    }

    private static PluginColor fromLegacyCodeOrDefault(char code, PluginColor fallback) {
        char normalizedCode = Character.toLowerCase(code);

        for (PluginColor color : values())
            if (color.legacyCode == normalizedCode)
                return color;

        return fallback;
    }
}
