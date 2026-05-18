package org.mcwonderland.uhc.scenario.impl.special;

import org.mcwonderland.uhc.api.event.player.UHCPlayerDamageEvent;
import org.mcwonderland.uhc.api.event.player.UHCPlayerRespawnedEvent;
import org.mcwonderland.uhc.api.event.timer.FinalHealEvent;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioIronMan extends ConfigBasedScenario implements Listener {
    private static final double MIN_MAX_HEALTH = 1.0;
    private static final Set<UHCPlayer> damaged = new HashSet<>();
    private static final Set<UHCPlayer> ironMen = new HashSet<>();

    @FilePath(name = "Extra_Heal")
    private Integer extraHeal;
    @FilePath(name = "Damage_Before_Final_Heal")
    private String damageBeforeFinalHeal;

    public ScenarioIronMan(ScenarioName name) {
        super(name);
    }


    @Override
    public void onDisable() {
        for (UHCPlayer uhcPlayer : new HashSet<>(ironMen)) {
            try {
                Player player = uhcPlayer.getPlayer();

                if (player != null)
                    changeMaxHealth(player, -extraHeal);
            } catch (RuntimeException | LinkageError ex) {
                logLifecycleFailure(ex, "restoring max health while disabling");
            }
        }
    }

    @Override
    public void onEnable() {
        for (UHCPlayer uhcPlayer : new HashSet<>(ironMen)) {
            Player player = uhcPlayer.getPlayer();
            changeMaxHealth(player, extraHeal);
        }
    }

    @EventHandler
    public void onFinalHeal(FinalHealEvent event) {
        try {
            for (UHCPlayer uhcPlayer : UHCPlayers.getBy(uhcPlayer -> !uhcPlayer.isDead() && !damaged.contains(uhcPlayer))) {
                if (changeMaxHealth(uhcPlayer.getEntity(), extraHeal))
                    ironMen.add(uhcPlayer);
            }
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling final heal");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    protected void checkDamage(UHCPlayerDamageEvent e) {
        try {
            if (!e.isCancelled()
                    && !PlayerUtils.isShieldBlocked(e.getEvent())
                    && !Game.getGame().isFinalHealEnabled()) {
                UHCPlayer uhcPlayer = e.getUhcPlayer();


                if (damaged.add(uhcPlayer))
                    Chat.send(uhcPlayer.getPlayer(), damageBeforeFinalHeal);
            }
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling player damage");
        }
    }

    @EventHandler
    public void onRespawn(UHCPlayerRespawnedEvent e) {
        try {
            UHCPlayer uhcPlayer = e.getUhcPlayer();

            if (ironMen.contains(uhcPlayer))
                changeMaxHealth(uhcPlayer.getEntity(), extraHeal);
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling player respawn");
        }
    }

    private boolean changeMaxHealth(LivingEntity entity, double delta) {
        if (entity == null)
            return false;

        Extra.setMaxHealth(entity, calculateAdjustedMaxHealth(Extra.getMaxHealth(entity), delta));
        return true;
    }

    static double calculateAdjustedMaxHealth(double currentMaxHealth, double delta) {
        return Math.max(MIN_MAX_HEALTH, currentMaxHealth + delta);
    }

    private void handleRuntimeFailure(Throwable throwable, String action) {
        PluginConsole.error(
                throwable,
                "Scenario 'Iron_Man' failed while " + action + ".",
                "The scenario was disabled for this run, but the game flow will continue."
        );
        disableAfterRuntimeFailure();
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            logLifecycleFailure(disableEx, "disabling after a runtime failure");
        }
    }

    private void logLifecycleFailure(Throwable throwable, String action) {
        PluginConsole.error(
                throwable,
                "Scenario 'Iron_Man' failed while " + action + "."
        );
    }

    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLore(list, "{heal}", extraHeal);
    }
}
