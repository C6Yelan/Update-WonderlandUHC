package org.mcwonderland.uhc.platform.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.platform.text.PluginText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class PluginItems {
    private static final String MINI_MESSAGE_FORMAT_TAGS = "black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white|obfuscated|bold|strikethrough|underlined|italic|reset|#[0-9a-f]{6}";
    private static final Pattern MINI_MESSAGE_FORMAT_PATTERN = Pattern.compile("(?i)</?(" + MINI_MESSAGE_FORMAT_TAGS + ")>");
    private static final Map<String, String> MATERIAL_NAME_ALIASES = Map.of(
            "CARROT_STICK", "CARROT_ON_A_STICK",
            "DIODE", "REPEATER",
            "EMPTY_MAP", "MAP",
            "ENDER_PORTAL_FRAME", "END_PORTAL_FRAME",
            "EXP_BOTTLE", "EXPERIENCE_BOTTLE",
            "FIREBALL", "FIRE_CHARGE",
            "NETHER_STALK", "NETHER_WART",
            "REDSTONE_TORCH_ON", "REDSTONE_TORCH",
            "ROSE_RED", "RED_DYE",
            "WATCH", "CLOCK"
    );

    private PluginItems() {
    }

    public static ItemStack fromConfig(String fileName, String path) {
        YamlConfiguration configuration = loadConfiguration(fileName);
        String materialName = configuration.getString(path + ".Type");

        if (materialName == null)
            throw new IllegalArgumentException("Missing item material at " + fileName + " " + path + ".Type");

        return create(parseMaterial(materialName, fileName, path), configuration.getString(path + ".Name"), configuration.getStringList(path + ".Lore"));
    }

    public static ItemStack fromConfig(String fileName, String path, Object... replacements) {
        YamlConfiguration configuration = loadConfiguration(fileName);
        String materialName = configuration.getString(path + ".Type");

        if (materialName == null)
            throw new IllegalArgumentException("Missing item material at " + fileName + " " + path + ".Type");

        return create(
                parseMaterial(materialName, fileName, path),
                PluginText.replaceToString(configuration.getString(path + ".Name"), replacements),
                PluginText.replaceToList(configuration.getStringList(path + ".Lore"), replacements)
        );
    }

    public static int slotFromConfig(String fileName, String path) {
        YamlConfiguration configuration = loadConfiguration(fileName);
        String slotPath = path + ".Slot";

        if (!configuration.isInt(slotPath))
            throw new IllegalArgumentException("Missing item slot at " + fileName + " " + slotPath);

        return configuration.getInt(slotPath);
    }

    public static ItemStack create(Material material, String name, List<String> lore) {
        return create(new ItemStack(material), name, lore, true);
    }

    public static ItemStack create(ItemStack baseItem, String name, List<String> lore, boolean hideTags) {
        ItemStack item = new ItemStack(baseItem);
        ItemMeta meta = item.getItemMeta();

        if (meta == null)
            return item;

        if (name != null && !name.isEmpty())
            meta.displayName(PluginText.toComponent(itemNameText(name)));

        if (lore != null && !lore.isEmpty())
            meta.lore(toLoreComponents(lore));

        if (hideTags)
            meta.addItemFlags(ItemFlag.values());

        item.setItemMeta(meta);
        return item;
    }

    private static List<Component> toLoreComponents(List<String> lore) {
        List<Component> components = new ArrayList<>();

        for (String line : lore) {
            if (line == null)
                continue;

            for (String subLine : line.split("\n"))
                components.add(PluginText.toComponent(itemLoreText(subLine)));
        }

        return components;
    }

    private static String itemNameText(String name) {
        return usesMiniMessage(name) ? name : "<reset><white>" + name + "</white>";
    }

    private static String itemLoreText(String lore) {
        return usesMiniMessage(lore) ? lore : "<gray>" + lore + "</gray>";
    }

    private static boolean usesMiniMessage(String text) {
        return MINI_MESSAGE_FORMAT_PATTERN.matcher(text).find();
    }

    private static Material parseMaterial(String materialName, String fileName, String path) {
        String normalizedName = normalizeMaterialName(materialName);

        try {
            return Material.valueOf(normalizedName);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid item material '" + materialName + "' at " + fileName + " " + path + ".Type", ex);
        }
    }

    private static String normalizeMaterialName(String materialName) {
        String normalizedName = materialName.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

        if (normalizedName.startsWith("MINECRAFT:"))
            normalizedName = normalizedName.substring("MINECRAFT:".length());

        return MATERIAL_NAME_ALIASES.getOrDefault(normalizedName, normalizedName);
    }

    private static YamlConfiguration loadConfiguration(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(WonderlandUHC.getInstance().getDataFolder(), fileName));
    }
}
