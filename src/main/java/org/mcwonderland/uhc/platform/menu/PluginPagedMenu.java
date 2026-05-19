package org.mcwonderland.uhc.platform.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.platform.text.PluginText;

import java.util.ArrayList;
import java.util.List;

public abstract class PluginPagedMenu<T> extends PluginMenu {
    private static final int MIN_PAGE_SIZE = 9;
    private static final int MAX_PAGE_SIZE = 45;

    private final List<T> items = new ArrayList<>();
    private final int pageSize;
    private int currentPage = 1;

    protected PluginPagedMenu(PluginMenuSection section, Iterable<T> items) {
        super(section);

        for (T item : items)
            this.items.add(item);

        this.pageSize = clamp(section.getSize() - 9, MIN_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        List<T> pageItems = getCurrentPageItems();

        if (slot < pageItems.size()) {
            ItemStack item = convertToItemStack(pageItems.get(slot));
            return item == null ? null : new ItemStack(item);
        }

        if (hasMultiplePages()) {
            if (slot == getPreviousButtonPosition())
                return createPreviousButton();

            if (slot == getNextButtonPosition())
                return createNextButton();
        }

        return null;
    }

    @Override
    protected final String getTitle() {
        String title = getSection().getTitle();
        if (!hasMultiplePages())
            return title;

        Component titleWithPage = PluginText.toComponent(title)
                .append(Component.text(" " + currentPage + "/" + getTotalPages(), NamedTextColor.DARK_GRAY));
        return PluginText.toMiniMessageString(titleWithPage);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        if (hasMultiplePages()) {
            if (slot == getPreviousButtonPosition()) {
                if (currentPage > 1) {
                    currentPage--;
                    displayTo(player);
                }

                return;
            }

            if (slot == getNextButtonPosition()) {
                if (currentPage < getTotalPages()) {
                    currentPage++;
                    displayTo(player);
                }

                return;
            }
        }

        List<T> pageItems = getCurrentPageItems();

        if (slot < pageItems.size())
            onPageClick(player, pageItems.get(slot), click);
    }

    protected abstract ItemStack convertToItemStack(T item);

    protected void onPageClick(Player player, T item, ClickType click) {
    }

    private ItemStack createPreviousButton() {
        boolean canGo = currentPage > 1;
        String name = canGo ? "<dark_gray><< </dark_gray><white>Page " + (currentPage - 1) + "</white>" : "<gray>First Page</gray>";

        return PluginItems.create(canGo ? Material.LIME_DYE : Material.GRAY_DYE, name, List.of());
    }

    private ItemStack createNextButton() {
        boolean canGo = currentPage < getTotalPages();
        String name = canGo ? "<white>Page " + (currentPage + 1) + " </white><dark_gray>>></dark_gray>" : "<gray>Last Page</gray>";

        return PluginItems.create(canGo ? Material.LIME_DYE : Material.GRAY_DYE, name, List.of());
    }

    private List<T> getCurrentPageItems() {
        int fromIndex = (currentPage - 1) * pageSize;

        if (fromIndex >= items.size())
            return List.of();

        return items.subList(fromIndex, Math.min(fromIndex + pageSize, items.size()));
    }

    private int getPreviousButtonPosition() {
        return getSection().getSize() - 6;
    }

    private int getNextButtonPosition() {
        return getSection().getSize() - 4;
    }

    private boolean hasMultiplePages() {
        return getTotalPages() > 1;
    }

    private int getTotalPages() {
        return Math.max(1, (items.size() + pageSize - 1) / pageSize);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
