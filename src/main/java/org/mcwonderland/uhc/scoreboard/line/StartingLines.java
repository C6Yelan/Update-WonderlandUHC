package org.mcwonderland.uhc.scoreboard.line;

import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.StateName;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.timer.Timers;

import java.util.List;

public class StartingLines extends LobbyLines {

    public StartingLines(List<String> lines) {
        super(lines);
    }

    @Override
    protected List<String> replaceGlobal(List<String> lines) {
        int teleported = Timers.SCATTER.getTeleportedNumbers();
        int teleporting = UHCTeam.getTeams().size() - teleported;

        return replaceLines(super.replaceGlobal(lines),
                "{start_in}", Game.getGame().getCurrentStateName() == StateName.TELEPORTING ? "--:--" : Timers.getUntilEnableFormat(Timers.START),
                "{teleporting}", teleporting,
                "{teleported}", teleported);
    }
}
