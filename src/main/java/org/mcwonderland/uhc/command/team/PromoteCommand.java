package org.mcwonderland.uhc.command.team;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;

class PromoteCommand extends TeamOwnerCommand {

    protected PromoteCommand(String sublabel) {
        super(sublabel);

        setMinArguments(1);
        setUsage("<玩家>");
        setDescription("將指定玩家升格為隊長。");
        setPermission(UHCPermission.COMMAND_TEAM_PROMOTE.toString());
    }

    @Override
    protected void onOwnerCommand() {
        checkModeAndGameStatus();

        UHCPlayer uhcPlayer = findUHCPlayer(args[0]);
        UHCTeam team = getTeam();

        checkExecuteSelf(uhcPlayer);
        checkInTeam(uhcPlayer);

        team.promote(uhcPlayer);
    }

    @Override
    boolean completesPlayerName() {
        return true;
    }
}
