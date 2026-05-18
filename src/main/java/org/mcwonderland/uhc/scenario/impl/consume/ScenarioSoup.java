package org.mcwonderland.uhc.scenario.impl.consume;

import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.scenario.impl.ConfigBasedScenario;
import org.mcwonderland.uhc.util.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 2019-12-07 下午 03:12
 */
public class ScenarioSoup extends ConfigBasedScenario implements Listener {

    @FilePath(name = "Regen_Health")
    private Integer soupRegenHealth;

    public ScenarioSoup(ScenarioName name) {
        super(name);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack itemInHand = e.getItem();

        if (needRegen(player) && isSoup(itemInHand))
            soupRegen(player, e.getHand(), itemInHand);
    }

    private boolean needRegen(Player player) {
        return player.getHealth() < PlayerUtils.getMaxHealth(player);
    }

    private boolean isSoup(ItemStack itemInHand) {
        return itemInHand != null && itemInHand.getType() == PluginMaterials.materialOf("MUSHROOM_STEW");
    }

    private void soupRegen(Player player, EquipmentSlot hand, ItemStack soupItem) {
        if (hand == null)
            return;

        player.getInventory().setItem(hand, soupItem.withType(PluginMaterials.materialOf("BOWL")));
        double maxHealth = PlayerUtils.getMaxHealth(player);
        double regeneratedHealth = player.getHealth() + soupRegenHealth;

        player.setHealth(Math.max(0, Math.min(regeneratedHealth, maxHealth)));
    }

    @Override
    protected List<String> replaceLore(List<String> list) {
        return replaceLore(list, "{heal}", soupRegenHealth);
    }
}
