package org.mcwonderland.uhc.scenario.impl.death;

import org.mcwonderland.uhc.events.UHCGamingDeathEvent;
import org.mcwonderland.uhc.model.InvinciblePlayer;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.PlayerUtils;
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
            PluginConsole.error(
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
        double damage = calculatePenaltyDamage(PlayerUtils.getMaxHealth(killer), PlayerUtils.getAbsorptionHearts(killer));
        /*
         * ShiftKill 懲罰是 scenario 規則，不應被 NoClean 的短暫無敵抵銷。
         * 這裡只繞過 InvinciblePlayer 的取消邏輯，不改變 Bukkit 原本的傷害 / 死亡事件流程。
         */
        if (damage > 0)
            InvinciblePlayer.runBypassingInvincibleDamageCancel(killer, () -> killer.damage(damage));
    }

    static double calculatePenaltyDamage(double maxHealth, double absorptionHearts) {
        return (Math.max(0, maxHealth) + Math.max(0, absorptionHearts)) / 2;
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            PluginConsole.error(
                    disableEx,
                    "Scenario 'Shift_Kill' could not be disabled after a runtime failure."
            );
        }
    }
}
