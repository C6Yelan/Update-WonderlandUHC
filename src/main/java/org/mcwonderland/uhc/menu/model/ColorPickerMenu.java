package org.mcwonderland.uhc.menu.model;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.menu.PluginPagedMenu;
import org.mcwonderland.uhc.platform.text.PluginColor;

import java.util.function.Consumer;

public abstract class ColorPickerMenu extends PluginPagedMenu<PluginColor> {
    private final Consumer<Player> returnToParent;

    public ColorPickerMenu(Consumer<Player> returnToParent) {
        super(PluginMenuSection.of("Color_Picker"), PluginColor.SELECTABLE);
        this.returnToParent = returnToParent;
    }

    @Override
    protected ItemStack convertToItemStack(PluginColor color) {
        return new ItemStack(color.toWoolMaterial());
    }

    @Override
    protected final void onPageClick(Player player, PluginColor color, ClickType click) {
        onChooseColor(player, color);
        returnToParent.accept(player);
    }

    protected abstract void onChooseColor(Player player, PluginColor color);
}
