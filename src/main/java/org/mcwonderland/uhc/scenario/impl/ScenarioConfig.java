package org.mcwonderland.uhc.scenario.impl;

import org.bukkit.Material;
import org.mcwonderland.uhc.scenario.annotation.FilePath;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mcwonderland.uhc.util.SoundConfigParser;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.settings.YamlConfig;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
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
        return parseMaterial(materialName, scenario.getName(), "Type");
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
            throw new IllegalArgumentException("Could not set scenario field '" + field.getName() + "' from " + scenario.getName() + "." + filePath.name(), e);
        } catch (RuntimeException | LinkageError ex) {
            throw new IllegalArgumentException("Invalid scenario config value at " + scenario.getName() + "." + filePath.name() + " for field '" + field.getName() + "'", ex);
        }
    }

    private void setValueByType(Field field, String path) throws IllegalAccessException {
        Class<?> type = field.getType();
        if (SimpleSound.class.isAssignableFrom(type))
            field.set(scenario, getScenarioSound(path));
        else if (Material.class.isAssignableFrom(type))
            field.set(scenario, getMaterialValue(path));
        else if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType genericType = ( ParameterizedType ) field.getGenericType();
            Class<?> typeClass = ( Class<?> ) genericType.getActualTypeArguments()[0];
            if (Material.class.isAssignableFrom(typeClass))
                field.set(scenario, getScenarioMaterialList(path));
            else
                field.set(scenario, getList(path, typeClass));
        } else
            field.set(scenario, get(path, type));
    }

    private Material getMaterialValue(String path) {
        Object material = getObject(path);
        return parseMaterial(String.valueOf(material), scenario.getName(), path);
    }

    private List<Material> getScenarioMaterialList(String path) {
        List<Material> materials = new ArrayList<>();

        for (Object material : getList(path))
            materials.add(parseMaterial(String.valueOf(material), scenario.getName(), path));

        return materials;
    }

    private SimpleSound getScenarioSound(String path) {
        Object sound = getObject(path);

        try {
            return SoundConfigParser.parse(String.valueOf(sound));
        } catch (RuntimeException | LinkageError ex) {
            throw new IllegalArgumentException("Invalid scenario sound '" + sound + "' in " + scenario.getName() + "." + path, ex);
        }
    }

    static Material parseMaterial(String materialName, String scenarioName, String path) {
        String normalizedName = normalizeMaterialName(materialName);

        try {
            return Material.valueOf(normalizedName);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid scenario material '" + materialName + "' in " + scenarioName + "." + path, ex);
        }
    }

    static List<Material> parseMaterialList(List<Object> materialNames, String scenarioName, String path) {
        List<Material> materials = new ArrayList<>();

        for (Object materialName : materialNames)
            materials.add(parseMaterial(String.valueOf(materialName), scenarioName, path));

        return materials;
    }

    static String normalizeSoundLine(String soundLine) {
        return SoundConfigParser.normalizeSoundLine(soundLine);
    }

    static String normalizeMaterialName(String materialName) {
        String normalizedName = normalizeNamespacedValue(materialName);

        return LEGACY_MATERIAL_ALIASES.getOrDefault(normalizedName, normalizedName);
    }

    private static String normalizeNamespacedValue(String value) {
        String normalizedName = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

        if (normalizedName.startsWith("MINECRAFT:"))
            normalizedName = normalizedName.substring("MINECRAFT:".length());

        return normalizedName;
    }

}
