package org.mcwonderland.uhc.scenario.impl.special;

import org.mcwonderland.uhc.api.event.player.UHCPlayerRespawnedEvent;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.PlayerUtils;
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

//todo open && close
public class ScenarioArmorVsHealth extends ConfigBasedScenario implements Listener {
    private static Map<UHCPlayer, Double> costs = new HashMap<>();

    @FilePath(name = "Apply_Within_Seconds_After_Respawned")
    private Integer Apply_Within_Seconds;
    @FilePath(name = "Warn_Msg")
    private List<String> Warn_Msg;

    public ScenarioArmorVsHealth(ScenarioName name) {
        super(name);
    }

    @EventHandler
    public void onRespawn(UHCPlayerRespawnedEvent e) {
        UHCPlayer uhcPlayer = e.getUhcPlayer();
        Player player = uhcPlayer.getPlayer();

        Chat.send(player, LegacyFoundationAdapter.replaceTimeToArray(Warn_Msg, Apply_Within_Seconds));

        LegacyFoundationAdapter.runLater(Apply_Within_Seconds * 20, () -> {
            if (!uhcPlayer.isDead()) {
                costs.remove(uhcPlayer);
                updateHealth(uhcPlayer);
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player)
            scheduleHealthUpdate(( Player ) event.getWhoClicked());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player)
            scheduleHealthUpdate(( Player ) event.getWhoClicked());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        scheduleHealthUpdate(event.getPlayer());
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        scheduleHealthUpdate(event.getPlayer());
    }

    private void scheduleHealthUpdate(Player player) {
        LegacyFoundationAdapter.runLater(0, () -> updateHealth(UHCPlayer.getUHCPlayer(player)));
    }

    @Override
    public void onDisable() {
        costs.clear();
    }

    private void updateHealth(UHCPlayer uhcPlayer) {
        LivingEntity livingEntity = uhcPlayer.getEntity();
        double armorPoints = PlayerUtils.getArmorPoints(livingEntity);
        double cost = getCost(uhcPlayer);

        if (armorPoints > cost)
            LegacyFoundationAdapter.runLater(0, () -> {
                double difference = armorPoints - cost;
                costs.put(uhcPlayer, armorPoints);

                Extra.setMaxHealth(livingEntity, Extra.getMaxHealth(livingEntity) - difference);
            });
    }

    private double getCost(UHCPlayer uhcPlayer) {
        return costs.getOrDefault(uhcPlayer, 0D);
    }
}
