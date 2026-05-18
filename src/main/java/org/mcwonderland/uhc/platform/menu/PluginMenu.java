package org.mcwonderland.uhc.platform.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.platform.text.PluginText;

public abstract class PluginMenu implements InventoryHolder {
    private final PluginMenuSection section;
    private Inventory inventory;

    protected PluginMenu(PluginMenuSection section) {
        this.section = section;
    }

    public final void displayTo(Player player) {
        inventory = Bukkit.createInventory(this, section.getSize(), PluginText.toComponent(getTitle()));

        for (int slot = 0; slot < inventory.getSize(); slot++)
            inventory.setItem(slot, getItemAt(slot));

        player.openInventory(inventory);
    }

    @Override
    public final Inventory getInventory() {
        return inventory;
    }

    final void handleClick(Player player, int slot, ClickType click, ItemStack clicked) {
        onClick(player, slot, click, clicked);
    }

    protected final PluginMenuSection getSection() {
        return section;
    }

    protected ItemStack getItemAt(int slot) {
        return null;
    }

    protected String getTitle() {
        return section.getTitle();
    }

    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
    }
}
