package org.mcwonderland.uhc.scenario.impl.death;

import org.mcwonderland.uhc.events.UHCGamingDeathEvent;
import org.mcwonderland.uhc.legacy.LegacyDatouNmsAdapter;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioShiftKill extends ConfigBasedScenario implements Listener {

    public ScenarioShiftKill(ScenarioName name) {
        super(name);
    }

    @EventHandler
    public void onGamingEntityDeath(UHCGamingDeathEvent e) {
        try {
            handleGamingEntityDeath(e);
        } catch (RuntimeException | LinkageError ex) {
            LegacyFoundationAdapter.error(
                    ex,
                    "Scenario 'Shift_Kill' failed while handling a death event.",
                    "The scenario was disabled for this run, but the death flow will continue."
            );
            disableAfterRuntimeFailure();
        }
    }

    private void handleGamingEntityDeath(UHCGamingDeathEvent e) {
        LivingEntity entity = e.getEntity();
        Player killer = entity.getKiller();

        if (killer == null)
            return;

        if (!killer.isSneaking())
            costHalfHealth(killer);
    }

    private void costHalfHealth(Player killer) {
        double damage = calculatePenaltyDamage(killer.getHealth(), LegacyDatouNmsAdapter.current().getAbsorptionHearts(killer));
        if (damage > 0)
            killer.damage(damage);
    }

    static double calculatePenaltyDamage(double health, double absorptionHearts) {
        return (Math.max(0, health) + Math.max(0, absorptionHearts)) / 2;
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            LegacyFoundationAdapter.error(
                    disableEx,
                    "Scenario 'Shift_Kill' could not be disabled after a runtime failure."
            );
        }
    }
}
