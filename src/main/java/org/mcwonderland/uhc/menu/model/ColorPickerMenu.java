package org.mcwonderland.uhc.menu.model;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.menu.PluginPagedMenu;

import java.util.List;
import java.util.function.Consumer;

public abstract class ColorPickerMenu extends PluginPagedMenu<ChatColor> {
    private static final List<ChatColor> COLORS = List.of(
            ChatColor.BLACK,
            ChatColor.DARK_BLUE,
            ChatColor.DARK_GREEN,
            ChatColor.DARK_AQUA,
            ChatColor.DARK_RED,
            ChatColor.DARK_PURPLE,
            ChatColor.GOLD,
            ChatColor.GRAY,
            ChatColor.DARK_GRAY,
            ChatColor.BLUE,
            ChatColor.GREEN,
            ChatColor.AQUA,
            ChatColor.RED,
            ChatColor.LIGHT_PURPLE,
            ChatColor.YELLOW,
            ChatColor.WHITE
    );

    private final Consumer<Player> returnToParent;

    public ColorPickerMenu(Consumer<Player> returnToParent) {
        super(PluginMenuSection.of("Color_Picker"), COLORS);
        this.returnToParent = returnToParent;
    }

    @Override
    protected ItemStack convertToItemStack(ChatColor chatColor) {
        return new ItemStack(toWoolMaterial(chatColor));
    }

    @Override
    protected final void onPageClick(Player player, ChatColor chatColor, ClickType click) {
        onChooseColor(player, chatColor);
        returnToParent.accept(player);
    }

    protected abstract void onChooseColor(Player player, ChatColor chatColor);

    private Material toWoolMaterial(ChatColor chatColor) {
        switch (chatColor) {
            case BLACK:
                return Material.BLACK_WOOL;
            case DARK_BLUE:
                return Material.BLUE_WOOL;
            case DARK_GREEN:
                return Material.GREEN_WOOL;
            case DARK_AQUA:
                return Material.CYAN_WOOL;
            case DARK_RED:
            case RED:
                return Material.RED_WOOL;
            case DARK_PURPLE:
                return Material.PURPLE_WOOL;
            case GOLD:
                return Material.ORANGE_WOOL;
            case GRAY:
                return Material.LIGHT_GRAY_WOOL;
            case DARK_GRAY:
                return Material.GRAY_WOOL;
            case BLUE:
                return Material.BLUE_WOOL;
            case GREEN:
                return Material.LIME_WOOL;
            case AQUA:
                return Material.LIGHT_BLUE_WOOL;
            case LIGHT_PURPLE:
                return Material.MAGENTA_WOOL;
            case YELLOW:
                return Material.YELLOW_WOOL;
            case WHITE:
            default:
                return Material.WHITE_WOOL;
        }
    }
}
