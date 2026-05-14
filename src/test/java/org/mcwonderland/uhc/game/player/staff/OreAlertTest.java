package org.mcwonderland.uhc.game.player.staff;

import org.bukkit.Material;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class OreAlertTest {

    @Test
    public void deepOreVariantsUseBaseOreAlert() {
        assertSame(OreAlert.DIAMOND_ORE, OreAlert.fromMaterial(Material.DEEPSLATE_DIAMOND_ORE));
        assertSame(OreAlert.GOLD_ORE, OreAlert.fromMaterial(Material.DEEPSLATE_GOLD_ORE));
    }
}
