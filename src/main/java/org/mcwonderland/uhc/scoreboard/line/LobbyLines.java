package org.mcwonderland.uhc.scoreboard.line;

import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.UHCGameSettings;
import org.mcwonderland.uhc.game.timer.Timers;

import java.util.List;

public class LobbyLines extends UHCLines {

    public LobbyLines(List<String> lines) {
        super(lines);
    }

    @Override
    protected List<String> replaceGlobal(List<String> lines) {
        UHCGameSettings settings = Game.getSettings();

        return replaceLines(super.replaceGlobal(lines),
                "{teleport_in}", Timers.getUntilEnableFormat(Timers.LOBBY),
                "{team_size}", settings.getTeamSettings().getTeamSize(),
                "{max_players}", settings.getMaxPlayers());
    }
}
