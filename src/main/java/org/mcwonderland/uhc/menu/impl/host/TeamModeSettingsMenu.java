package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.api.enums.TeamSplitMode;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.sub.UHCTeamSettings;
import org.mcwonderland.uhc.platform.item.PluginItems;
import org.mcwonderland.uhc.platform.console.PluginConsole;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.settings.UHCFiles;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;

public class TeamModeSettingsMenu extends PluginMenu {
    private static final String SECTION = "Teams";
    private static final String SIZE_BUTTON = "Size";
    private static final String TEAM_FIRE_BUTTON = "Team_Fire";
    private static final String TEAM_SPLIT_MODE_BUTTON = "Team_Split_Mode";
    private static final int BACK_OFFSET = 1;
    private static final Object ENABLED_STATUS = PluginText.formatted("<green>On</green>");
    private static final Object DISABLED_STATUS = PluginText.formatted("<red>Off</red>");

    public TeamModeSettingsMenu() {
        super(PluginMenuSection.of(SECTION));
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        UHCTeamSettings teamSettings = Game.getSettings().getTeamSettings();

        if (slot == getBackButtonSlot())
            return PluginItems.fromConfig(UHCFiles.MENUS, "Leave");

        if (slot == getSection().getButtonSlot(SIZE_BUTTON))
            return getSection().getButtonItem(SIZE_BUTTON, "{count}", teamSettings.getTeamSize());

        if (slot == getSection().getButtonSlot(TEAM_FIRE_BUTTON))
            return getSection().getButtonItem(TEAM_FIRE_BUTTON, "{status}", teamSettings.isAllowTeamFire() ? ENABLED_STATUS : DISABLED_STATUS);

        if (slot == getSection().getButtonSlot(TEAM_SPLIT_MODE_BUTTON))
            return getSection().getButtonItem(TEAM_SPLIT_MODE_BUTTON, "{type}", teamSettings.getTeamSplitMode().name());

        return super.getItemAt(slot);
    }

    @Override
    protected void onClick(Player player, int slot, ClickType click, ItemStack clicked) {
        UHCTeamSettings teamSettings = Game.getSettings().getTeamSettings();

        if (slot == getBackButtonSlot()) {
            new MainSettingsMenu().displayTo(player);
            return;
        }

        if (slot == getSection().getButtonSlot(SIZE_BUTTON)) {
            handleTeamSizeClick(player, click, teamSettings);
            return;
        }

        if (slot == getSection().getButtonSlot(TEAM_FIRE_BUTTON)) {
            toggleTeamFire(player, teamSettings);
            return;
        }

        if (slot == getSection().getButtonSlot(TEAM_SPLIT_MODE_BUTTON))
            handleSplitModeClick(player, click, teamSettings);
    }

    private void handleTeamSizeClick(Player player, ClickType click, UHCTeamSettings teamSettings) {
        if (click == ClickType.LEFT) {
            teamSettings.setTeamSize(Math.max(1, teamSettings.getTeamSize() - 1));
            saveCurrentSettings();
            displayTo(player);
            return;
        }

        if (click == ClickType.RIGHT) {
            teamSettings.setTeamSize(teamSettings.getTeamSize() + 1);
            saveCurrentSettings();
            displayTo(player);
        }
    }

    private void toggleTeamFire(Player player, UHCTeamSettings teamSettings) {
        boolean newStatus = !teamSettings.isAllowTeamFire();
        String message = (newStatus ? Messages.Host.TEAM_FIRE_ENABLED_PLAYER : Messages.Host.TEAM_FIRE_DISABLED_PLAYER)
                .replace("{player}", player.getName());

        teamSettings.setAllowTeamFire(newStatus);
        saveCurrentSettings();
        Chat.broadcast(message);
        PluginConsole.log(message);
        Extra.sound(player, Sounds.Host.SCENARIO_TOGGLED);
        displayTo(player);
    }

    private void handleSplitModeClick(Player player, ClickType click, UHCTeamSettings teamSettings) {
        if (click == ClickType.LEFT) {
            teamSettings.setTeamSplitMode(TeamSplitMode.CHOSEN);
            saveCurrentSettings();
            displayTo(player);
            return;
        }

        if (click == ClickType.RIGHT) {
            teamSettings.setTeamSplitMode(TeamSplitMode.RANDOM);
            saveCurrentSettings();
            displayTo(player);
        }
    }

    private int getBackButtonSlot() {
        return getSection().getSize() - BACK_OFFSET;
    }

    private static void saveCurrentSettings() {
        WorldLoadingCacheState.saveCache();
    }
}
