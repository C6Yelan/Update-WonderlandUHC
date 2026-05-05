package org.mcwonderland.uhc.core.match;

import org.junit.Test;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.game.settings.sub.UHCTeamSettings;

import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyMatchSettingsMapperTest {

    @Test
    public void mapsLegacySettingsToMatchSettings() {
        LinkedHashSet<String> scenarios = new LinkedHashSet<>();
        scenarios.add("cutclean");
        scenarios.add("noclean");

        UHCTeamSettings teamSettings = new UHCTeamSettings();
        teamSettings.setTeamSize(4);
        teamSettings.setAllowTeamFire(true);

        UHCGameSettings legacySettings = new UHCGameSettings();
        legacySettings.setTitle("Custom UHC");
        legacySettings.setMaxPlayers(64);
        legacySettings.setTeamSettings(teamSettings);
        legacySettings.setUsingNether(true);
        legacySettings.setGenerator("default");
        legacySettings.setScenarios(scenarios);

        MatchSettings settings = LegacyMatchSettingsMapper.fromGameSettings(legacySettings);

        assertEquals("Custom UHC", settings.getTitle());
        assertEquals(64, settings.getMaxPlayers());
        assertEquals(4, settings.getTeamSize());
        assertTrue(settings.isAllowTeamFire());
        assertTrue(settings.isUsingNether());
        assertEquals("default", settings.getGenerator());
        assertEquals(scenarios, settings.getScenarios());
    }

    @Test(expected = IllegalArgumentException.class)
    public void legacySettingsCannotBeNull() {
        LegacyMatchSettingsMapper.fromGameSettings(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void legacyTeamSettingsCannotBeNull() {
        UHCGameSettings legacySettings = new UHCGameSettings();

        LegacyMatchSettingsMapper.fromGameSettings(legacySettings);
    }
}
