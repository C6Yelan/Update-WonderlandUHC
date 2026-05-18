package org.mcwonderland.uhc.util;

import org.bukkit.Sound;
import org.mcwonderland.uhc.platform.sound.PluginSound;

import java.util.Locale;
import java.util.Map;

public final class SoundConfigParser {

    private static final Map<String, String> LEGACY_SOUND_ALIASES = Map.ofEntries(
            Map.entry("ANVIL_USE", "BLOCK_ANVIL_USE"),
            Map.entry("BURP", "ENTITY_PLAYER_BURP"),
            Map.entry("CHICKEN_EGG_POP", "ENTITY_CHICKEN_EGG"),
            Map.entry("CLICK", "UI_BUTTON_CLICK"),
            Map.entry("DIG_SAND", "BLOCK_SAND_BREAK"),
            Map.entry("DOOR_CLOSE", "BLOCK_WOODEN_DOOR_CLOSE"),
            Map.entry("DRINK", "ENTITY_GENERIC_DRINK"),
            Map.entry("EAT", "ENTITY_GENERIC_EAT"),
            Map.entry("ENDERMAN_TELEPORT", "ENTITY_ENDERMAN_TELEPORT"),
            Map.entry("FIREWORK_LARGE_BLAST", "ENTITY_FIREWORK_ROCKET_LARGE_BLAST"),
            Map.entry("LEVEL_UP", "ENTITY_PLAYER_LEVELUP"),
            Map.entry("NOTE_BASS", "BLOCK_NOTE_BLOCK_BASS"),
            Map.entry("NOTE_PLING", "BLOCK_NOTE_BLOCK_PLING"),
            Map.entry("NOTE_STICKS", "BLOCK_NOTE_BLOCK_HAT"),
            Map.entry("ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP"),
            Map.entry("WITHER_DEATH", "ENTITY_WITHER_DEATH")
    );

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

        return LEGACY_SOUND_ALIASES.getOrDefault(normalizedName, normalizedName);
    }

    private static String[] splitSoundLine(String soundLine) {
        return soundLine.contains(", ") ? soundLine.split(", ") : soundLine.split(" ");
    }
}
