package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.util.TeamModifier;

import java.util.List;

public class SplitTeamCommand extends UHCSubCommand {

    protected SplitTeamCommand(String sublabel) {
        super(sublabel);

        setUsage("[是否要先解散所有隊伍(true/false)]");
        setDescription("對玩家進行分隊。");
        setPermission(UHCPermission.COMMAND_UHC_SPLITTEAM.toString());
    }

    @Override
    protected void onCommand() {
        if (args.length == 1 && Boolean.parseBoolean(args[0])) {
            TeamModifier.resetTeams();
        }

        TeamModifier.splitTeams();
    }

    @Override
    List<String> tabComplete(String[] args) {

        if (args.length == 1)
            return completeLastWord(args, "false", "true");

        return List.of();
    }
}
