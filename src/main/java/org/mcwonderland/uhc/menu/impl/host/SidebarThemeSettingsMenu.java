package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.menu.PluginPagedMenu;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.scoreboard.SidebarTheme;
import org.mcwonderland.uhc.scoreboard.line.ScoreLines;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Extra;

import java.util.List;

public class SidebarThemeSettingsMenu extends PluginPagedMenu<SidebarTheme> {
    private static final String SECTION = "Sidebar_Theme_Selector";
    private static final String THEMES_BUTTON = "Themes";


    private final UHCPlayer uhcPlayer;

    public SidebarThemeSettingsMenu(Player player) {
        super(PluginMenuSection.of(SECTION), SidebarTheme.getAllThemes());

        this.uhcPlayer = UHCPlayer.getUHCPlayer(player);
    }

    @Override
    protected ItemStack convertToItemStack(SidebarTheme theme) {
        return getSection().getButtonItem(
                THEMES_BUTTON,
                "{theme_name}", theme.getName(),
                "{theme_preview}", PluginText.formatted(formatPreview(getTestLinesIn(theme).getFor(uhcPlayer)))
        );
    }

    private String formatPreview(List<String> lines) {
        return String.join("\n", lines);
    }

    private ScoreLines getTestLinesIn(SidebarTheme theme) {
        ScoreLines lobbyLines = theme.getLobbyLines();

        updateVarPreventNullPointer(lobbyLines);

        return lobbyLines;
    }

    private void updateVarPreventNullPointer(ScoreLines lobbyLines) {
        lobbyLines.updateGlobalVariables();
    }

    @Override
    protected void onPageClick(Player player, SidebarTheme sidebarTheme, ClickType clickType) {
        Game.getSettings().getScoreboardSettings().setSidebarTheme(sidebarTheme);
        Extra.sound(player, Sounds.Host.SCENARIO_TOGGLED);
        new ScoreboardSettingsMenu().displayTo(player);
    }
}
