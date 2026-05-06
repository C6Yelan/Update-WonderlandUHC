package org.mcwonderland.uhc.scenario.impl;

import com.google.common.collect.Lists;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.scenario.ScenarioName;
import org.mineacademy.fo.menu.model.ItemCreator;

import java.util.Collection;
import java.util.List;

public abstract class ConfigBasedScenario extends AbstractScenario {

    public ConfigBasedScenario(ScenarioName name) {
        super(name.capitalize(), LegacyFoundationAdapter.itemOf("AIR"));
    }

    @Override
    protected final void onReload() {
        ScenarioConfig config = getNewConfig();

        config.loadFieldValues();
        onConfigReload();

        setIcon(ItemCreator.of(
                config.getMaterial(),
                config.getFancyName(),
                getDesc(config.getDescription())
        ).make());
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
        return LegacyFoundationAdapter.replaceToList(description);
    }

    protected final List<String> replaceLore(List<String> description, Object... replacements) {
        return LegacyFoundationAdapter.replaceToList(description, replacements);
    }

    protected final List<String> replaceLoreJoined(List<String> description, String placeholder, Collection<?> values, String delimiter) {
        return LegacyFoundationAdapter.replaceJoinedToList(description, placeholder, values, delimiter);
    }

    protected final List<String> replaceLoreTime(List<String> description, Number seconds) {
        return LegacyFoundationAdapter.replaceTimeToList(description, seconds);
    }

    private ScenarioConfig getNewConfig() {
        return new ScenarioConfig(this);
    }
}
