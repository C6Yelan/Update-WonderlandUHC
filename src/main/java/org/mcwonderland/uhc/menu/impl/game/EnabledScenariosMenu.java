package org.mcwonderland.uhc.menu.impl.game;

import org.mcwonderland.uhc.api.Scenario;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.menu.PluginPagedMenu;
import org.mcwonderland.uhc.scenario.ScenarioManager;
import org.bukkit.inventory.ItemStack;

/**
 * 2019-11-27 上午 10:19
 */
public class EnabledScenariosMenu extends PluginPagedMenu<Scenario> {

    public EnabledScenariosMenu(ScenarioManager manager) {
        super(PluginMenuSection.of("Enabled_Scenarios"), manager.getEnabledScenarios());
    }

    @Override
    protected ItemStack convertToItemStack(Scenario scenario) {
        return scenario.getIcon();
    }
}
