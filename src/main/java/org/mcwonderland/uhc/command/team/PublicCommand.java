package org.mcwonderland.uhc.command.team;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.settings.CommandSettings;

class PublicCommand extends TeamOwnerCommand {

    protected PublicCommand(TeamCommandGroup parent, String sublabel) {
        super(parent, sublabel);

        setDescription("公開隊伍讓其他玩家自由加入。");
        setPermission(UHCPermission.COMMAND_TEAM_PUBLIC.toString());
    }

    @Override
    protected void onOwnerCommand() {
        checkModeAndGameStatus();

        UHCTeam team = getTeam();
        team.setOpenJoin(!team.isOpenJoin());
        tell(team.isOpenJoin() ? CommandSettings.Team.Public.OPENED : CommandSettings.Team.Public.CLOSED);
    }
}
