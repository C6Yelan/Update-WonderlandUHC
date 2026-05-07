package org.mcwonderland.uhc.scenario.impl;

import org.bukkit.Material;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.settings.YamlConfig;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScenarioConfig extends YamlConfig {

    private static final Map<String, String> LEGACY_MATERIAL_ALIASES = Map.of(
            "COOKED_FISH", "COOKED_COD",
            "ENCHANTMENT_TABLE", "ENCHANTING_TABLE",
            "MUSHROOM_SOUP", "MUSHROOM_STEW",
            "WEB", "COBWEB",
            "WORKBENCH", "CRAFTING_TABLE"
    );

    private ConfigBasedScenario scenario;

    public ScenarioConfig(ConfigBasedScenario scenario) {
        setPathPrefix(scenario.getName());
        this.scenario = scenario;
        loadConfiguration(UHCFiles.SCENARIOS);
    }

    public Material getMaterial() {
        String materialName = getString("Type");
        String normalizedName = normalizeMaterialName(materialName);

        try {
            return Material.valueOf(normalizedName);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid scenario icon material '" + materialName + "' in " + scenario.getName(), ex);
        }
    }

    public String getFancyName() {
        return getString("Name");
    }

    public List<String> getDescription() {
        return getStringList("Description");
    }

    public void loadFieldValues() {
        Field[] fields = scenario.getClass().getDeclaredFields();

        for (Field field : fields) {
            FilePath filePath = field.getAnnotation(FilePath.class);

            if (filePath != null)
                setValueFromFile(field, filePath);
        }
    }

    private void setValueFromFile(Field field, FilePath filePath) {
        try {
            field.setAccessible(true);
            setValueByType(field, filePath.name());
        } catch (IllegalAccessException e) {
            LegacyFoundationAdapter.error(e, "Could't set the field value '" + field.getName() + "' in " + scenario.getName());
        }
    }

    private void setValueByType(Field field, String path) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (SimpleSound.class.isAssignableFrom(type))
            field.set(scenario, getSound(path));
        else if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType genericType = ( ParameterizedType ) field.getGenericType();
            Class<?> typeClass = ( Class<?> ) genericType.getActualTypeArguments()[0];
            field.set(scenario, getList(path, typeClass));
        } else
            field.set(scenario, get(path, type));
    }

    private String normalizeMaterialName(String materialName) {
        String normalizedName = materialName.toUpperCase(Locale.ROOT);

        if (normalizedName.startsWith("MINECRAFT:"))
            normalizedName = normalizedName.substring("MINECRAFT:".length());

        return LEGACY_MATERIAL_ALIASES.getOrDefault(normalizedName, normalizedName);
    }

}
