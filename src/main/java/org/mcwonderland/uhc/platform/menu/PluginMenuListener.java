package org.mcwonderland.uhc.platform.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class PluginMenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        PluginMenu menu = getMenu(event.getView().getTopInventory());

        if (menu == null)
            return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;

        int rawSlot = event.getRawSlot();

        if (rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize())
            menu.handleClick(player, rawSlot, event.getClick(), event.getCurrentItem());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        PluginMenu menu = getMenu(event.getView().getTopInventory());

        if (menu == null)
            return;

        int topSize = event.getView().getTopInventory().getSize();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private PluginMenu getMenu(Inventory inventory) {
        if (inventory == null || !(inventory.getHolder() instanceof PluginMenu menu))
            return null;

        return menu;
    }
}
