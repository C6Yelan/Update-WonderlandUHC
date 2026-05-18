package org.mcwonderland.uhc.platform.menu;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.settings.UHCFiles;

import java.io.File;
import java.util.List;

public final class PluginMenuSection {
    private final String path;
    private final YamlConfiguration configuration;

    private PluginMenuSection(String path) {
        this.path = path;
        this.configuration = YamlConfiguration.loadConfiguration(new File(WonderlandUHC.getInstance().getDataFolder(), UHCFiles.MENUS));
    }

    public static PluginMenuSection of(String path) {
        return new PluginMenuSection(path);
    }

    public String getTitle() {
        return configuration.getString(path + ".Title", "");
    }

    public int getSize() {
        int rows = configuration.getInt(path + ".Rows");

        if (rows <= 0)
            throw new IllegalArgumentException("Missing menu rows at " + UHCFiles.MENUS + " " + path + ".Rows");

        return rows * 9;
    }

    public int getButtonSlot(String buttonName) {
        String buttonPath = buttonPath(buttonName);

        if (!configuration.isInt(buttonPath + ".Slot"))
            throw new IllegalArgumentException("Missing menu button slot at " + UHCFiles.MENUS + " " + buttonPath + ".Slot");

        return configuration.getInt(buttonPath + ".Slot");
    }

    public ItemStack getButtonItem(String buttonName, Object... replacements) {
        return PluginItems.fromConfig(UHCFiles.MENUS, buttonPath(buttonName), replacements);
    }

    public String getButtonName(String buttonName) {
        return configuration.getString(buttonPath(buttonName) + ".Name");
    }

    public List<String> getButtonLore(String buttonName) {
        return configuration.getStringList(buttonPath(buttonName) + ".Lore");
    }

    private String buttonPath(String buttonName) {
        return path + ".Buttons." + buttonName;
    }
}
