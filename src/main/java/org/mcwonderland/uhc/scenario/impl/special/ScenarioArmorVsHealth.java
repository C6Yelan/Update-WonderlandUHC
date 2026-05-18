package org.mcwonderland.uhc.scenario.impl.special;

import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.mcwonderland.uhc.api.event.player.UHCPlayerRespawnedEvent;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//todo open && close
public class ScenarioArmorVsHealth extends ConfigBasedScenario implements Listener {
    private static final double MIN_MAX_HEALTH = 1.0;
    private static Map<UUID, Double> costs = new HashMap<>();
    private static Map<UUID, Double> healthReductions = new HashMap<>();

    @FilePath(name = "Apply_Within_Seconds_After_Respawned")
    private Integer Apply_Within_Seconds;
    @FilePath(name = "Warn_Msg")
    private List<String> Warn_Msg;

    public ScenarioArmorVsHealth(ScenarioName name) {
        super(name);
    }

    @EventHandler
    public void onRespawn(UHCPlayerRespawnedEvent e) {
        try {
            handleRespawn(e);
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling a respawn event");
        }
    }

    private void handleRespawn(UHCPlayerRespawnedEvent e) {
        UHCPlayer uhcPlayer = e.getUhcPlayer();
        Player player = uhcPlayer.getPlayer();

        Chat.send(player, PluginText.replaceTimeToArray(Warn_Msg, Apply_Within_Seconds));

        PluginScheduler.runLater(Apply_Within_Seconds * 20, () -> {
            try {
                if (!uhcPlayer.isDead()) {
                    costs.remove(uhcPlayer);
                    updateHealth(uhcPlayer);
                }
            } catch (RuntimeException | LinkageError ex) {
                handleRuntimeFailure(ex, "applying delayed respawn health");
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            if (event.getWhoClicked() instanceof Player)
                scheduleHealthUpdate(( Player ) event.getWhoClicked());
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling an inventory click");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        try {
            if (event.getWhoClicked() instanceof Player)
                scheduleHealthUpdate(( Player ) event.getWhoClicked());
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling an inventory drag");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            scheduleHealthUpdate(event.getPlayer());
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling a player interaction");
        }
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        try {
            scheduleHealthUpdate(event.getPlayer());
        } catch (RuntimeException | LinkageError ex) {
            handleRuntimeFailure(ex, "handling an item break");
        }
    }

    private void scheduleHealthUpdate(Player player) {
        PluginScheduler.runLater(0, () -> {
            try {
                updateHealth(UHCPlayer.getUHCPlayer(player));
            } catch (RuntimeException | LinkageError ex) {
                handleRuntimeFailure(ex, "applying an equipment health update");
            }
        });
    }

    @Override
    public void onDisable() {
        restoreReducedHealth();
        costs.clear();
        healthReductions.clear();
    }

    private void restoreReducedHealth() {
        for (Map.Entry<UUID, Double> entry : new HashMap<>(healthReductions).entrySet()) {
            try {
                Player player = Bukkit.getPlayer(entry.getKey());

                if (player != null)
                    Extra.setMaxHealth(player, calculateRestoredMaxHealth(Extra.getMaxHealth(player), entry.getValue()));
            } catch (RuntimeException | LinkageError ex) {
                handleRuntimeFailure(ex, "restoring max health while disabling");
            }
        }
    }

    private void updateHealth(UHCPlayer uhcPlayer) {
        if (uhcPlayer == null)
            return;

        LivingEntity livingEntity = uhcPlayer.getEntity();
        if (livingEntity == null)
            return;

        double armorPoints = PlayerUtils.getArmorPoints(livingEntity);
        double cost = getCost(uhcPlayer);

        if (armorPoints > cost) {
            double difference = armorPoints - cost;
            double currentMaxHealth = Extra.getMaxHealth(livingEntity);
            double updatedMaxHealth = calculateReducedMaxHealth(currentMaxHealth, difference);
            double appliedReduction = Math.max(0, currentMaxHealth - updatedMaxHealth);

            costs.put(uhcPlayer.getUniqueId(), armorPoints);
            healthReductions.merge(uhcPlayer.getUniqueId(), appliedReduction, Double::sum);

            Extra.setMaxHealth(livingEntity, updatedMaxHealth);
        }
    }

    private double getCost(UHCPlayer uhcPlayer) {
        return costs.getOrDefault(uhcPlayer.getUniqueId(), 0D);
    }

    static double calculateReducedMaxHealth(double currentMaxHealth, double difference) {
        if (difference <= 0)
            return Math.max(MIN_MAX_HEALTH, currentMaxHealth);

        return Math.max(MIN_MAX_HEALTH, currentMaxHealth - difference);
    }

    static double calculateRestoredMaxHealth(double currentMaxHealth, double reduction) {
        if (reduction <= 0)
            return Math.max(MIN_MAX_HEALTH, currentMaxHealth);

        return Math.max(MIN_MAX_HEALTH, currentMaxHealth + reduction);
    }

    private void handleRuntimeFailure(Throwable throwable, String action) {
        PluginConsole.error(
                throwable,
                "Scenario 'Armor_Vs_Health' failed while " + action + ".",
                "The scenario was disabled for this run, but the game flow will continue."
        );
        disableAfterRuntimeFailure();
    }

    private void disableAfterRuntimeFailure() {
        try {
            if (isEnabled())
                disable();
        } catch (RuntimeException | LinkageError disableEx) {
            PluginConsole.error(
                    disableEx,
                    "Scenario 'Armor_Vs_Health' could not be disabled after a runtime failure."
            );
        }
    }
}
