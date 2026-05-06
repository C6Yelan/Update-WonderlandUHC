package org.mcwonderland.uhc.command.team;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Extra;

class CreateCommand extends TeamSubCommand {

    protected CreateCommand(TeamCommandGroup parent, String sublabel) {
        super(parent, sublabel);

        setDescription("創建隊伍。");
        setPermission(UHCPermission.COMMAND_TEAM_CREATE.toString());
    }

    @Override
    protected void onCommand() {
        checkModeAndGameStatus();
        checkAlreadyInTeam();

        UHCTeam.createTeamIfNotExist(getUhcPlayer());
        tell(LegacyFoundationAdapter.replaceToArray(
                CommandSettings.Team.Create.CREATED,
                "{cmd}", getLabel()));

        Extra.sound(getPlayer(), Sounds.Commands.TEAM_CREATED);
    }
}
