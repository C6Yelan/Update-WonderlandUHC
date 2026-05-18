package org.mcwonderland.uhc.command.team;

import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.command.CommandHelper;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.settings.CommandSettings;

class KickCommand extends TeamOwnerCommand {

    protected KickCommand(TeamCommandGroup parent, String sublabel) {
        super(parent, sublabel);

        setMinArguments(1);
        setUsage("<玩家>");
        setDescription("將指定玩家從隊伍中剔除。");
        setPermission(UHCPermission.COMMAND_TEAM_KICK.toString());
    }

    @Override
    protected void onOwnerCommand() {
        checkModeAndGameStatus();

        UHCTeam team = getTeam();
        UHCPlayer target = CommandHelper.findUHCPlayer(args[0]);

        checkExecuteSelf(target);
        checkInTeam(target);

        team.sendMessage(PluginText.replaceToArray(
                CommandSettings.Team.Kick.KICKED,
                "{player}", target.getName()));

        team.leave(target);
    }
}
