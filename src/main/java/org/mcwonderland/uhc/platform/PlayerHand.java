package org.mcwonderland.uhc.platform;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@UtilityClass
public class PlayerHand {

    public ItemStack getMainHandItem(Player player) {
        return player.getInventory().getItemInMainHand();
    }
}
