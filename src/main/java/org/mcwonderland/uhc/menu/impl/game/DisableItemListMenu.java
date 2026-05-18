package org.mcwonderland.uhc.menu.impl.game;

import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.menu.PluginPagedMenu;
import org.bukkit.inventory.ItemStack;

public class DisableItemListMenu extends PluginPagedMenu<ItemStack> {


    public DisableItemListMenu() {
        super(PluginMenuSection.of("Disable_Item_List"),
                Game.getSettings().getItemSettings().getCustomDisabledItems());
    }

    @Override
    protected ItemStack convertToItemStack(ItemStack item) {
        return item;
    }
}
