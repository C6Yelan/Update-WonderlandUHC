package org.mcwonderland.uhc.command.team;

import org.mcwonderland.uhc.UHCPermission;

class DisbandCommand extends TeamOwnerCommand {

    protected DisbandCommand(String sublabel) {
        super(sublabel);

        setDescription("解散目前隊伍。");
        setPermission(UHCPermission.COMMAND_TEAM_DISBAND.toString());
    }

    @Override
    protected void onOwnerCommand() {
        checkModeAndGameStatus();

        getTeam().disband();
    }
}
