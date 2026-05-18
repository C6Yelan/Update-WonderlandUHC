package org.mcwonderland.uhc.scoreboard;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.scoreboard.line.GameLines;
import org.mcwonderland.uhc.scoreboard.line.LobbyLines;
import org.mcwonderland.uhc.scoreboard.line.SoloLines;
import org.mcwonderland.uhc.scoreboard.line.StaffLines;
import org.mcwonderland.uhc.scoreboard.line.StartingLines;
import org.mcwonderland.uhc.scoreboard.line.TeamsLines;
import org.mcwonderland.uhc.scoreboard.line.UHCLines;
import org.mcwonderland.uhc.settings.UHCFiles;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class SidebarTheme {

    private static final List<SidebarTheme> themes = new ArrayList<>();

    private final String name;
    private final Set<UHCLines> lines = new HashSet<>();

    private UHCLines lobbyLines, startingLines;
    private UHCLines spectatorSoloLines, spectatorTeamsLines;
    private UHCLines staffSoloLines, staffTeamsLines;
    private UHCLines playerSoloLines, playerTeamsLines;

    public static SidebarTheme defaultTheme() {
        return themes.get(0);
    }

    public static SidebarTheme getThemeOrDefault(String name) {
        return themes.stream()
                .filter(sidebarTheme -> sidebarTheme.getName().equalsIgnoreCase(name))
                .findFirst().orElse(defaultTheme());
    }

    public static Collection<SidebarTheme> getAllThemes() {
        return themes;
    }

    public static void loadThemes() {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(scoreboardsFile());

        themes.clear();

        for (String key : configuration.getKeys(false))
            themes.add(new SidebarTheme(key, configuration.getConfigurationSection(key)));
    }

    public SidebarTheme(String sectionPrefix) {
        this(sectionPrefix, YamlConfiguration.loadConfiguration(scoreboardsFile()).getConfigurationSection(sectionPrefix));
    }

    private SidebarTheme(String name, ConfigurationSection section) {
        this.name = name;
        loadScoreboardModels(section);
    }

    public String getName() {
        return name;
    }

    private void loadScoreboardModels(ConfigurationSection section) {
        lobbyLines = new LobbyLines(lines(section, "Lobby"));
        startingLines = new StartingLines(lines(section, "Starting"));
        spectatorSoloLines = new GameLines(lines(section, "Spectator_Solo"));
        spectatorTeamsLines = new GameLines(lines(section, "Spectator_Teams"));
        staffSoloLines = new StaffLines(lines(section, "Staff_Solo"));
        staffTeamsLines = new StaffLines(lines(section, "Staff_Teams"));
        playerSoloLines = new SoloLines(lines(section, "Player_Solo"));
        playerTeamsLines = new TeamsLines(lines(section, "Player_Teams"));

        lines.addAll(Arrays.asList(
                lobbyLines,
                startingLines,
                spectatorSoloLines,
                spectatorTeamsLines,
                staffSoloLines,
                staffTeamsLines,
                playerSoloLines,
                playerTeamsLines
        ));
    }

    private static List<String> lines(ConfigurationSection section, String path) {
        return section == null ? List.of() : section.getStringList(path);
    }

    private static File scoreboardsFile() {
        return new File(WonderlandUHC.getInstance().getDataFolder(), UHCFiles.SCOREBOARDS);
    }
}
