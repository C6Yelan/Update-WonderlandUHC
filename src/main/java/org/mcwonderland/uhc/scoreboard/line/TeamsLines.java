package org.mcwonderland.uhc.scoreboard.line;

import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;

import java.util.List;

public class TeamsLines extends SoloLines {

    public TeamsLines(List<String> lines) {
        super(lines);
    }

    @Override
    protected List<String> replace(UHCPlayer uhcPlayer, List<String> lines) {
        List<String> replacedLines = super.replace(uhcPlayer, lines);
        UHCTeam team = uhcPlayer.getTeam();

        if (team == null)
            return replacedLines;

        return replaceLines(replacedLines,
                "{team_name}", team.getName(),
                "{team_color}", team.getColor(),
                "{team_kills}", team.getKills());

    }
}
