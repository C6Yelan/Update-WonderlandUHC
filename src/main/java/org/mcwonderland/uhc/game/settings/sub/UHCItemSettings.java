package org.mcwonderland.uhc.game.settings.sub;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.mcwonderland.uhc.model.InventoryContent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UHCItemSettings {
    private InventoryContent customInventoryItems;
    private InventoryContent practiceInventoryItems;
    private List<ItemStack> customDrops;
    private List<ItemStack> customDisabledItems;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("Custom_Disabled_Items", customDisabledItems);
        map.put("Practice_Inventory_Items", practiceInventoryItems.toMap());
        map.put("Custom_Inventory_Items", customInventoryItems.toMap());
        map.put("Custom_Drops", customDrops);

        return map;
    }

    public static UHCItemSettings fromSection(ConfigurationSection section) {
        UHCItemSettings uhcItemSettings = new UHCItemSettings();

        uhcItemSettings.customDisabledItems = InventoryContent.itemStackList(section == null ? List.of() : section.getList("Custom_Disabled_Items", List.of()));
        uhcItemSettings.customDrops = InventoryContent.itemStackList(section == null ? List.of() : section.getList("Custom_Drops", List.of()));
        uhcItemSettings.customInventoryItems = InventoryContent.fromSection(section == null ? null : section.getConfigurationSection("Custom_Inventory_Items"));
        uhcItemSettings.practiceInventoryItems = InventoryContent.fromSection(section == null ? null : section.getConfigurationSection("Practice_Inventory_Items"));

        return uhcItemSettings;
    }
}
