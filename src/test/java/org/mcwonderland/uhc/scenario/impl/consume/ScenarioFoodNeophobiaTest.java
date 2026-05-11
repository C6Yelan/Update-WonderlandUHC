package org.mcwonderland.uhc.scenario.impl.consume;

import org.bukkit.Material;
import org.junit.After;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ScenarioFoodNeophobiaTest {

    @After
    public void tearDown() {
        ScenarioFoodNeophobia.clearEatenFoodTypes();
    }

    @Test
    public void clearEatenFoodTypesRemovesPreviousPlayerChoices() {
        UUID playerId = UUID.randomUUID();

        ScenarioFoodNeophobia.recordEatenFoodType(playerId, Material.APPLE);
        assertSame(Material.APPLE, ScenarioFoodNeophobia.getEatenFoodType(playerId));

        ScenarioFoodNeophobia.clearEatenFoodTypes();

        assertNull(ScenarioFoodNeophobia.getEatenFoodType(playerId));
    }
}
