package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.util.TeamModifier;

public class ResetTeamCommand extends UHCSubCommand {

    protected ResetTeamCommand(String sublabel) {
        super(sublabel);

        setDescription("強制解散所有隊伍。");
        setPermission(UHCPermission.COMMAND_UHC_RESETTEAM.toString());
    }

    @Override
    protected void onCommand() {
        checkWaiting();

        TeamModifier.resetTeams();
    }
}
