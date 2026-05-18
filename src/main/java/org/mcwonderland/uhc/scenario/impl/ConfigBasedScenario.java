package org.mcwonderland.uhc.scenario.impl;

import org.mcwonderland.uhc.platform.material.PluginMaterials;
import org.mcwonderland.uhc.platform.text.PluginText;
import com.google.common.collect.Lists;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mineacademy.fo.menu.model.ItemCreator;

import java.util.Collection;
import java.util.List;

public abstract class ConfigBasedScenario extends AbstractScenario {

    public ConfigBasedScenario(ScenarioName name) {
        super(name.capitalize(), PluginMaterials.itemOf("AIR"));
    }

    @Override
    protected final void onReload() {
        ScenarioConfig config = getNewConfig();

        config.loadFieldValues();
        onConfigReload();

        setIcon(ItemCreator.of(new ItemStack(config.getMaterial()))
                .name(config.getFancyName())
                .lore(getDesc(config.getDescription()))
                .hideTags(true)
                .make());
    }

    private List<String> getDesc(List<String> description) {
        try {
            return replaceLore(description);
        } catch (Exception ex) {
            ex.printStackTrace();
            return Lists.newArrayList();
        }
    }


    protected void onConfigReload() {

    }

    protected List<String> replaceLore(List<String> description) {
        return PluginText.replaceToList(description);
    }

    protected final List<String> replaceLore(List<String> description, Object... replacements) {
        return PluginText.replaceToList(description, replacements);
    }

    protected final List<String> replaceLoreJoined(List<String> description, String placeholder, Collection<?> values, String delimiter) {
        return PluginText.replaceJoinedToList(description, placeholder, values, delimiter);
    }

    protected final List<String> replaceLoreTime(List<String> description, Number seconds) {
        return PluginText.replaceTimeToList(description, seconds);
    }

    private ScenarioConfig getNewConfig() {
        return new ScenarioConfig(this);
    }
}
