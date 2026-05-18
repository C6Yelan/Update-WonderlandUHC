package org.mcwonderland.uhc.model;

import com.google.common.collect.Lists;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryContent {
    private List<ItemStack> storage;
    private List<ItemStack> armor;
    private List<ItemStack> extra;

    public InventoryContent(Player player) {
        this(player.getInventory());
    }

    public InventoryContent(PlayerInventory inv) {
        this.storage = getStorageContent(inv);
        this.extra = getExtraContent(inv);
        this.armor = Arrays.asList(inv.getArmorContents().clone());
    }

    private InventoryContent() {
        this.storage = Lists.newArrayList();
        this.extra = Lists.newArrayList();
        this.armor = Lists.newArrayList();
    }

    public static InventoryContent empty() {
        return new InventoryContent();
    }

    public static ItemStack[] contentsOf(Player player) {
        return contentsOf(player.getInventory());
    }

    public static ItemStack[] contentsOf(PlayerInventory inventory) {
        return new InventoryContent(inventory).getAllItems();
    }

    private List<ItemStack> getStorageContent(PlayerInventory inv) {
        //getContents() returns all contents in version above 1_9_R2,
        // or it's gonna returns storage only.
        //This class is made to solve this problem.

        try {
            return Arrays.asList(inv.getStorageContents().clone());
        } catch (NoSuchMethodError e) {
            return Arrays.asList(inv.getContents());
        }
    }

    private List<ItemStack> getExtraContent(PlayerInventory inv) {
        try {
            return Arrays.asList(inv.getExtraContents().clone());
        } catch (NoSuchMethodError e) {
            return Arrays.asList(new ItemStack[0]);
        }
    }

    public void setContents(Player player) {
        setContents(player.getInventory());
    }

    public void setContents(PlayerInventory inv) {
        setStorageContents(inv);
        setExtraContents(inv);
        inv.setArmorContents(armor.toArray(new ItemStack[0]));
    }

    public ItemStack[] getAllItems() {
        return Extra.mergeArrays(storage.toArray(new ItemStack[0]), armor.toArray(new ItemStack[0]), extra.toArray(new ItemStack[0]));
    }

    private void setExtraContents(PlayerInventory inventory) {
        try {
            inventory.setExtraContents(extra.toArray(new ItemStack[0]));
        } catch (NoSuchMethodError e) {

        }
    }

    private void setStorageContents(PlayerInventory inventory) {
        try {
            inventory.setStorageContents(storage.toArray(new ItemStack[0]));
        } catch (NoSuchMethodError e) {
            inventory.setContents(storage.toArray(new ItemStack[0]));
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("Storage", storage);
        map.put("Armor", armor);
        map.put("Extra", extra);

        return map;
    }

    public static InventoryContent fromSection(ConfigurationSection section) {
        InventoryContent inventoryContent = new InventoryContent();

        if (section == null)
            return inventoryContent;

        inventoryContent.storage = itemStackList(section.getList("Storage", List.of()));
        inventoryContent.armor = itemStackList(section.getList("Armor", List.of()));
        inventoryContent.extra = itemStackList(section.getList("Extra", List.of()));

        return inventoryContent;
    }

    public static List<ItemStack> itemStackList(List<?> values) {
        List<ItemStack> items = Lists.newArrayList();

        for (Object value : values)
            items.add(toItemStack(value));

        return items;
    }

    private static ItemStack toItemStack(Object value) {
        if (value == null)
            return null;

        if (value instanceof ItemStack item)
            return item;

        if (value instanceof ConfigurationSection section)
            return toItemStack(section.getValues(false));

        if (value instanceof Map<?, ?> map)
            return ItemStack.deserialize(stringKeyMap(map));

        throw new IllegalArgumentException("Unsupported item stack value: " + value.getClass().getName());
    }

    private static Map<String, Object> stringKeyMap(Map<?, ?> map) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet())
            values.put(String.valueOf(entry.getKey()), entry.getValue());

        return values;
    }
}
