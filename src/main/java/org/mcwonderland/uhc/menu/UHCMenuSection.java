package org.mcwonderland.uhc.menu;

import org.mcwonderland.uhc.settings.UHCFiles;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mineacademy.fo.menu.config.MenuSection;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.io.File;
import java.io.IOException;

public class UHCMenuSection extends MenuSection {
    private static final String SCENARIOS_SECTION = "Scenarios";
    private static final int MIN_SCENARIO_MENU_ROWS = 6;

    private UHCMenuSection(String sectionPrefix) {
        super(UHCFiles.MENUS, sectionPrefix);
    }

    public static UHCMenuSection of(String sectionPrefix) {
        migrateScenarioMenuRows(sectionPrefix);

        return new UHCMenuSection(sectionPrefix);
    }

    private static void migrateScenarioMenuRows(String sectionPrefix) {
        if (!SCENARIOS_SECTION.equals(sectionPrefix))
            return;

        File file = new File(SimplePlugin.getInstance().getDataFolder(), UHCFiles.MENUS);
        if (!file.exists())
            return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int rows = config.getInt(SCENARIOS_SECTION + ".Rows", MIN_SCENARIO_MENU_ROWS);

        if (rows >= MIN_SCENARIO_MENU_ROWS)
            return;

        config.set(SCENARIOS_SECTION + ".Rows", MIN_SCENARIO_MENU_ROWS);

        try {
            config.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not update " + UHCFiles.MENUS + " " + SCENARIOS_SECTION + ".Rows to " + MIN_SCENARIO_MENU_ROWS, ex);
        }
    }
}
