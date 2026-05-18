package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;

import java.util.List;

class SwitchTeamCommand extends UHCSubCommand {

    public SwitchTeamCommand(String sublabel) {
        super(sublabel);

        setMinArguments(2);
        setUsage("<目標> <要切換到哪位玩家的隊伍>");
        setDescription("切換玩家的隊伍。");
        setPermission(UHCPermission.COMMAND_UHC_SWITCHTEAM.toString());
    }

    @Override
    protected void onCommand() {
        UHCPlayer target = findUHCPlayer(args[0]);
        UHCPlayer toSwitch = findUHCPlayer(args[1]);
        UHCTeam team = toSwitch.getTeam();

        team.join(target);
    }

    @Override
    List<String> tabComplete(String[] args) {
        if (args.length == 1 || args.length == 2)
            return completePlayerNames(args);

        return List.of();
    }
}
