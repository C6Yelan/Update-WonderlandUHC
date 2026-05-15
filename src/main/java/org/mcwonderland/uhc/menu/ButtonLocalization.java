package org.mcwonderland.uhc.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.menu.button.ButtonReturnBack;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.YamlConfig;

import java.util.List;
import java.util.stream.Collectors;

public class ButtonLocalization extends YamlConfig {

    public static void load() {
        new ButtonLocalization();
    }

    private ButtonLocalization() {
        loadConfiguration(UHCFiles.MENUS);

        setupReturnBackButton();
        setupPageToggleButtons();
    }

    private void setupReturnBackButton() {
        ItemStack item = getItem("Leave");
        ItemMeta meta = item.getItemMeta();

        ButtonReturnBack.setMaterial(CompMaterial.fromMaterial(item.getType()));
        ButtonReturnBack.setTitle(getDisplayName(meta));
        ButtonReturnBack.setLore(getLore(meta));
    }

    private void setupPageToggleButtons() {
//        MenuPagged.setNextPageItemModel(getItem("Next_Page"));
//        MenuPagged.setFirstPageItemModel(getItem("First_Page"));
//        MenuPagged.setPrevPageItemModel(getItem("Previous_Page"));
//        MenuPagged.setLastPageItemModel(getItem("Last_Page"));
    }

    private ItemStack getItem(String path) {
        path = path + ".";

        return ItemCreator.of(
                getMaterial(path + "Type")
                , getString(path + "Name"),
                getStringList(path + "Lore")
        ).make();
    }

    private String getDisplayName(ItemMeta meta) {
        if (meta == null || meta.displayName() == null)
            return "";

        return LegacyComponentSerializer.legacySection().serialize(meta.displayName());
    }

    private List<String> getLore(ItemMeta meta) {
        if (meta == null || meta.lore() == null)
            return null;

        return meta.lore().stream()
                .map(this::toLegacyString)
                .collect(Collectors.toList());
    }

    private String toLegacyString(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
}
