package org.mcwonderland.uhc.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SoundConfigParserTest {

    @Test
    public void soundAliasesResolveModernBukkitSounds() {
        assertEquals("ENTITY_GENERIC_DRINK 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("DRINK 1.0F 1.0F"));
        assertEquals("ENTITY_PLAYER_LEVELUP 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("LEVEL_UP 1.0F 1.0F"));
        assertEquals("ENTITY_PLAYER_BURP 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("BURP 1.0F 1.0F"));
        assertEquals("ENTITY_EXPERIENCE_ORB_PICKUP 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("ORB_PICKUP 1.0F 1.0F"));
        assertEquals("BLOCK_NOTE_BLOCK_BASS 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("NOTE_BASS 1.0F 1.0F"));
        assertEquals("BLOCK_NOTE_BLOCK_PLING 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("NOTE_PLING 1.0F 1.0F"));
        assertEquals("BLOCK_NOTE_BLOCK_HAT 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("NOTE_STICKS 1.0F 1.0F"));
        assertEquals("BLOCK_ANVIL_USE 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("ANVIL_USE 1.0F 1.0F"));
        assertEquals("BLOCK_WOODEN_DOOR_CLOSE 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("DOOR_CLOSE 1.0F 1.0F"));
        assertEquals("ENTITY_FIREWORK_ROCKET_LARGE_BLAST 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("FIREWORK_LARGE_BLAST 1.0F 1.0F"));
    }

    @Test
    public void soundLineAliasesOnlyChangeSoundName() {
        assertEquals("ENTITY_PLAYER_LEVELUP 1.0F 2.0F", SoundConfigParser.normalizeSoundLine("LEVEL_UP 1.0F 2.0F"));
        assertEquals("UI_BUTTON_CLICK 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("CLICK 1.0F 1.0F"));
        assertEquals("ENTITY_ENDERMAN_TELEPORT 1.0F 1.0F", SoundConfigParser.normalizeSoundLine("ENDERMAN_TELEPORT 1.0F 1.0F"));
        assertEquals("none", SoundConfigParser.normalizeSoundLine("none"));
    }

}
