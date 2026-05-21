package org.mcwonderland.uhc.util;

import org.bukkit.Sound;
import org.mcwonderland.uhc.platform.sound.PluginSound;

import java.util.Locale;

public final class SoundConfigParser {

    private SoundConfigParser() {
    }

    public static PluginSound parse(String soundLine) {
        String normalizedLine = normalizeSoundLine(soundLine);

        if ("none".equalsIgnoreCase(normalizedLine))
            return PluginSound.none();

        String[] parts = splitSoundLine(normalizedLine);

        if (parts.length == 1)
            return PluginSound.of(resolveSound(parts[0]), 1F, 1.5F);

        if (parts.length != 3)
            throw new IllegalArgumentException("Malformed sound type, use format: 'sound' OR 'sound volume pitch'. Got: " + soundLine);

        if ("random".equalsIgnoreCase(parts[2]))
            return PluginSound.ofRandomPitch(resolveSound(parts[0]), Float.parseFloat(parts[1]));

        return PluginSound.of(resolveSound(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
    }

    public static String normalizeSoundLine(String soundLine) {
        if ("none".equalsIgnoreCase(soundLine.trim()))
            return "none";

        String separator = soundLine.contains(", ") ? ", " : " ";
        String[] parts = soundLine.split(separator);

        if (parts.length == 0)
            return soundLine;

        parts[0] = normalizeSoundName(parts[0]);

        return String.join(separator, parts);
    }

    @SuppressWarnings("removal")
    private static Sound resolveSound(String soundName) {
        String normalizedName = normalizeSoundName(soundName);

        try {
            return Sound.valueOf(normalizedName);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Sound '" + soundName + "' does not exist in this Minecraft version.", ex);
        }
    }

    private static String normalizeSoundName(String soundName) {
        String normalizedName = soundName.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

        if (normalizedName.startsWith("MINECRAFT:"))
            normalizedName = normalizedName.substring("MINECRAFT:".length());

        return normalizedName;
    }

    private static String[] splitSoundLine(String soundLine) {
        return soundLine.contains(", ") ? soundLine.split(", ") : soundLine.split(" ");
    }
}
