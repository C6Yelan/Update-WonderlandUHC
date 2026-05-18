package org.mcwonderland.uhc.tools;

import org.mcwonderland.uhc.settings.UHCFiles;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.util.ItemSimilarChecker;

import java.util.ArrayList;
import java.util.List;

public abstract class UHCTool {

    private static final List<UHCTool> TOOLS = new ArrayList<>();

    private final ItemStack item;
    private final int slot;

    protected UHCTool(String path) {
        item = PluginItems.fromConfig(UHCFiles.ITEMS, path);
        slot = PluginItems.slotFromConfig(UHCFiles.ITEMS, path);
        TOOLS.add(this);
    }

    public final void set(Player player) {
        player.getInventory().setItem(slot, getItem());
    }

    public final ItemStack getItem() {
        return new ItemStack(item);
    }

    public static UHCTool findTool(ItemStack itemStack) {
        for (UHCTool tool : TOOLS) {
            if (ItemSimilarChecker.isSimilar(tool.item, itemStack))
                return tool;
        }

        return null;
    }

    public static boolean isTool(ItemStack itemStack) {
        return findTool(itemStack) != null;
    }

    public final void handleInteract(PlayerInteractEvent event) {
        final Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)
            onRightClick(event);
    }

    protected abstract void onRightClick(PlayerInteractEvent event);
}
