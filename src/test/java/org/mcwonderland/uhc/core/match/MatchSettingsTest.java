package org.mcwonderland.uhc.core.match;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MatchSettingsTest {

    @Test
    public void defaultsProvideMinimalMatchSettings() {
        MatchSettings settings = MatchSettings.defaults();

        assertEquals("&a&lWonderland&f&lUHC", settings.getTitle());
        assertEquals(100, settings.getMaxPlayers());
        assertEquals(1, settings.getTeamSize());
        assertFalse(settings.isAllowTeamFire());
        assertFalse(settings.isUsingNether());
        assertEquals("", settings.getGenerator());
        assertTrue(settings.getScenarios().isEmpty());
    }

    @Test
    public void customSettingsExposeConfiguredValues() {
        Set<String> scenarios = new LinkedHashSet<>(Arrays.asList("CutClean", "TimeBomb"));
        MatchSettings settings = new MatchSettings("Custom UHC", 40, 2, true, true, "default", scenarios);

        assertEquals("Custom UHC", settings.getTitle());
        assertEquals(40, settings.getMaxPlayers());
        assertEquals(2, settings.getTeamSize());
        assertTrue(settings.isAllowTeamFire());
        assertTrue(settings.isUsingNether());
        assertEquals("default", settings.getGenerator());
        assertEquals(scenarios, settings.getScenarios());
    }

    @Test
    public void scenariosAreReadOnlyFromOutside() {
        MatchSettings settings = new MatchSettings("Custom UHC", 40, 2, true, true, "default", new LinkedHashSet<>(Arrays.asList("CutClean")));

        try {
            settings.getScenarios().add("TimeBomb");
        } catch (UnsupportedOperationException expected) {
            return;
        }

        throw new AssertionError("scenarios should be unmodifiable.");
    }

    @Test
    public void scenariosAreCopiedAtConstructionTime() {
        Set<String> scenarios = new LinkedHashSet<>(Arrays.asList("CutClean"));
        MatchSettings settings = new MatchSettings("Custom UHC", 40, 2, true, true, "default", scenarios);

        scenarios.add("TimeBomb");

        assertEquals(1, settings.getScenarios().size());
        assertTrue(settings.getScenarios().contains("CutClean"));
        assertFalse(settings.getScenarios().contains("TimeBomb"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void titleCannotBeBlank() {
        new MatchSettings(" ", 40, 2, true, true, "default", new LinkedHashSet<String>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void maxPlayersMustBePositive() {
        new MatchSettings("Custom UHC", 0, 2, true, true, "default", new LinkedHashSet<String>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void teamSizeMustBePositive() {
        new MatchSettings("Custom UHC", 40, 0, true, true, "default", new LinkedHashSet<String>());
    }
}
