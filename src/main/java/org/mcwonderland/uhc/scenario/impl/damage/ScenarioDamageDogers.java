package org.mcwonderland.uhc.scenario.impl.damage;

import org.mcwonderland.uhc.api.event.player.UHCPlayerDamageEvent;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mineacademy.fo.model.SimpleSound;

import java.util.List;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioDamageDogers extends ConfigBasedScenario implements Listener {
    private static int numberOfDead;
    private static final double MIN_LETHAL_DAMAGE = 2048.0D;

    @FilePath(name = "Amount")
    private Integer amount;
    @FilePath(name = "Death_Cause_This")
    private String deathCauseThis;
    @FilePath(name = "Death_Cause_This_Sound")
    private SimpleSound deathCauseThisSound;

    public ScenarioDamageDogers(ScenarioName name) {
        super(name);
    }

    @Override
    protected void onEnable() {
        resetNumberOfDead();
    }

    @Override
    protected void onDisable() {
        resetNumberOfDead();
    }

    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLore(list, "{amount}", amount);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(UHCPlayerDamageEvent e) {
        try {
            if (dontNeedMoreToDie())
                return;

            if (PlayerUtils.isShieldBlocked(e.getEvent()))
                return;

            killGamingEntity(e);
        } catch (RuntimeException | LinkageError ex) {
            PluginConsole.error(
                    ex,
                    "Scenario 'Damage_Dogers' failed while handling a damage event.",
                    "The scenario was disabled for this run, but the damage flow will continue."
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
                    "Scenario 'Damage_Dogers' could not be disabled after a runtime failure."
            );
        }
    }

    private boolean dontNeedMoreToDie() {
        return getNumberOfDead() >= amount;
    }

    private void killGamingEntity(UHCPlayerDamageEvent e) {
        LivingEntity entity = e.getUhcPlayer().getEntity();
        int remaining = recordDeathAndGetRemaining(amount);
        e.setDamage(calculateLethalDamage(entity.getHealth()));
        Chat.broadcast(deathCauseThis
                .replace("{player}", entity.getName())
                .replace("{amount}", remaining + "")
        );
        Extra.sound(deathCauseThisSound);
    }

    static int recordDeathAndGetRemaining(int amount) {
        numberOfDead++;
        return amount - numberOfDead;
    }

    static int getNumberOfDead() {
        return numberOfDead;
    }

    static double calculateLethalDamage(double health) {
        return Math.max(MIN_LETHAL_DAMAGE, Math.max(0.0D, health));
    }

    static void resetNumberOfDead() {
        numberOfDead = 0;
    }
}
