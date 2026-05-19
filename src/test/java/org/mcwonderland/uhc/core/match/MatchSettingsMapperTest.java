package org.mcwonderland.uhc.core.match;

import org.junit.Test;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.game.settings.sub.UHCTeamSettings;

import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatchSettingsMapperTest {

    @Test
    public void mapsGameSettingsToMatchSettings() {
        LinkedHashSet<String> scenarios = new LinkedHashSet<>();
        scenarios.add("cutclean");
        scenarios.add("noclean");

        UHCTeamSettings teamSettings = new UHCTeamSettings();
        teamSettings.setTeamSize(4);
        teamSettings.setAllowTeamFire(true);

        UHCGameSettings gameSettings = new UHCGameSettings();
        gameSettings.setTitle("Custom UHC");
        gameSettings.setMaxPlayers(64);
        gameSettings.setTeamSettings(teamSettings);
        gameSettings.setUsingNether(true);
        gameSettings.setGenerator("default");
        gameSettings.setScenarios(scenarios);

        MatchSettings settings = MatchSettingsMapper.fromGameSettings(gameSettings);

        assertEquals("Custom UHC", settings.getTitle());
        assertEquals(64, settings.getMaxPlayers());
        assertEquals(4, settings.getTeamSize());
        assertTrue(settings.isAllowTeamFire());
        assertTrue(settings.isUsingNether());
        assertEquals("default", settings.getGenerator());
        assertEquals(scenarios, settings.getScenarios());
    }

    @Test(expected = IllegalArgumentException.class)
    public void gameSettingsCannotBeNull() {
        MatchSettingsMapper.fromGameSettings(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void gameTeamSettingsCannotBeNull() {
        UHCGameSettings gameSettings = new UHCGameSettings();

        MatchSettingsMapper.fromGameSettings(gameSettings);
    }
}
