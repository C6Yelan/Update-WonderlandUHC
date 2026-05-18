package org.mcwonderland.uhc.menu.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.model.InventoryContent;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.util.Extra;

public class InventoryViewer extends PluginMenu {

    private final Player target;
    private final ItemStack[] items;

    public InventoryViewer(Player target) {
        super(PluginMenuSection.of("See_Inventory"));

        this.target = target;
        this.items = new ItemStack[getSection().getSize()];

        setInfoItem("Health", "{health}", Extra.formatHealth(target.getHealth()));
        setInfoItem("Hunger", "{hunger}", target.getFoodLevel());
        setInfoItem("Level", "{level}", target.getLevel());
        renderInventoryContents();
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        ItemStack item = items[slot];
        return item == null ? null : new ItemStack(item);
    }

    @Override
    protected String getTitle() {
        return PluginText.replaceToString(getSection().getTitle(), "{player}", target.getName());
    }

    private void setInfoItem(String buttonName, String placeholder, Object value) {
        items[getSection().getButtonSlot(buttonName)] = getSection().getButtonItem(buttonName, placeholder, value);
    }

    private void renderInventoryContents() {
        for (ItemStack content : InventoryContent.contentsOf(target))
            pushItem(content);
    }

    private void pushItem(ItemStack item) {
        if (item == null)
            return;

        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] == null) {
                items[slot] = new ItemStack(item);
                return;
            }
        }

        items[items.length - 1] = new ItemStack(item);
    }
}
