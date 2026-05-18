package org.mcwonderland.uhc.scenario.impl.block;

import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.events.UHCBlockBreakEvent;
import org.mcwonderland.uhc.model.VeinMiner;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.cuboid.SelectMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioTimber extends ConfigBasedScenario implements Listener {

    public ScenarioTimber(ScenarioName name) {
        super(name);
    }

    @EventHandler
    protected void onBlockBreak(UHCBlockBreakEvent e) {
        try {
            if (PluginMaterials.isLog(e.getBlockType()))
                VeinMiner.mineVeins(e.getBlock(), e.getPlayer(), SelectMode.CUBE);
        } catch (RuntimeException | LinkageError ex) {
            PluginConsole.error(
                    ex,
                    "Scenario 'Timber' failed while handling a block break event.",
                    "The scenario was disabled for this run, but the block break flow will continue."
            );
            disableAfterRuntimeFailure();
        }
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            PluginConsole.error(
                    disableEx,
                    "Scenario 'Timber' could not be disabled after a runtime failure."
            );
        }
    }
}
