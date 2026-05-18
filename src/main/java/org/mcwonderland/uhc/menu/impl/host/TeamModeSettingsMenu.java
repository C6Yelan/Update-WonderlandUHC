package org.mcwonderland.uhc.menu.impl.host;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mcwonderland.uhc.api.enums.TeamSplitMode;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.sub.UHCTeamSettings;
import org.mcwonderland.uhc.platform.menu.PluginMenu;
import org.mcwonderland.uhc.platform.menu.PluginMenuSection;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;

public class TeamModeSettingsMenu extends PluginMenu {
    private static final String SECTION = "Teams";
    private static final String SIZE_BUTTON = "Size";
    private static final String TEAM_FIRE_BUTTON = "Team_Fire";
    private static final String TEAM_SPLIT_MODE_BUTTON = "Team_Split_Mode";
    private static final String ENABLED_STATUS = "&aOn";
    private static final String DISABLED_STATUS = "&cOff";

    public TeamModeSettingsMenu() {
        super(PluginMenuSection.of(SECTION));
    }

    @Override
    protected ItemStack getItemAt(int slot) {
        UHCTeamSettings teamSettings = Game.getSettings().getTeamSettings();

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
            displayTo(player);
            return;
        }

        if (click == ClickType.RIGHT) {
            teamSettings.setTeamSize(teamSettings.getTeamSize() + 1);
            displayTo(player);
        }
    }

    private void toggleTeamFire(Player player, UHCTeamSettings teamSettings) {
        boolean newStatus = !teamSettings.isAllowTeamFire();

        teamSettings.setAllowTeamFire(newStatus);
        Chat.broadcast((newStatus ? Messages.Host.TEAM_FIRE_ENABLED_PLAYER : Messages.Host.TEAM_FIRE_DISABLED_PLAYER)
                .replace("{player}", player.getName()));
        displayTo(player);
    }

    private void handleSplitModeClick(Player player, ClickType click, UHCTeamSettings teamSettings) {
        if (click == ClickType.LEFT) {
            teamSettings.setTeamSplitMode(TeamSplitMode.CHOSEN);
            displayTo(player);
            return;
        }

        if (click == ClickType.RIGHT) {
            teamSettings.setTeamSplitMode(TeamSplitMode.RANDOM);
            displayTo(player);
        }
    }
}
