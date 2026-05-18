package org.mcwonderland.uhc.scenario.impl.consume;

import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.mcwonderland.uhc.platform.sound.PluginSound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioFoodNeophobia extends ConfigBasedScenario implements Listener {
    private static final Map<UUID, Material> eatenFoodType = new HashMap<>();

    @FilePath(name = "First_Eat")
    private String firstEatMsg;
    @FilePath(name = "Just_Can_Eat")
    private String justCanEatMsg;

    @FilePath(name = "First_Eat_Sound")
    private PluginSound firstEatSound;
    @FilePath(name = "Just_Can_Eat_Sound")
    private PluginSound justCanEatSound;


    public ScenarioFoodNeophobia(ScenarioName name) {
        super(name);
    }

    @Override
    protected void onEnable() {
        clearEatenFoodTypes();
    }

    @Override
    protected void onDisable() {
        clearEatenFoodTypes();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        Material itemEat = e.getItem().getType();

        if (isIgnoredItem(itemEat))
            return;

        Material foodEaten = getEatFoodType(player);
        if (foodEaten != null)
            e.setCancelled(!checkEatSame(player, foodEaten, itemEat));
        else
            putFoodData(player, itemEat);
    }

    private boolean checkEatSame(Player player, Material foodEaten, Material itemEat) {
        if (itemEat != foodEaten) {
            Chat.send(player, justCanEatMsg.replace("{foodtype}", foodEaten.name()));
            Extra.sound(player, justCanEatSound);
            return false;
        }
        return true;
    }

    private void putFoodData(Player player, Material itemEat) {
        recordEatenFoodType(player.getUniqueId(), itemEat);
        Chat.send(player, firstEatMsg.replace("{foodtype}", itemEat.name()));
        Extra.sound(player, firstEatSound);
    }

    private Material getEatFoodType(Player player) {
        return getEatenFoodType(player.getUniqueId());
    }

    private boolean isIgnoredItem(Material material) {
        return material == PluginMaterials.materialOf("MILK_BUCKET")
                || material == PluginMaterials.materialOf("GOLDEN_APPLE")
                || material == PluginMaterials.materialOf("ENCHANTED_GOLDEN_APPLE")
                || material == PluginMaterials.materialOf("POTION");
    }

    static void recordEatenFoodType(UUID playerId, Material material) {
        eatenFoodType.put(playerId, material);
    }

    static Material getEatenFoodType(UUID playerId) {
        return eatenFoodType.get(playerId);
    }

    static void clearEatenFoodTypes() {
        eatenFoodType.clear();
    }
}
