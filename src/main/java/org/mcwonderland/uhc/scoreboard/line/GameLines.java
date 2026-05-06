package org.mcwonderland.uhc.scoreboard.line;

import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameTimerRunnable;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.game.timer.Timers;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;

import java.util.List;

public class GameLines extends UHCLines {

    public GameLines(List<String> lines) {
        super(lines);
    }

    @Override
    protected List<String> replaceGlobal(List<String> lines) {
        int allTeams = UHCTeam.getTeams().size();
        int aliveTeams = UHCTeam.getAliveTeams().size();

        return replaceLines(super.replaceGlobal(lines),
                "{remaining}", UHCPlayers.countOf(uhcPlayer -> !uhcPlayer.isDead()),
                "{alive_teams}", aliveTeams,
                "{all_teams}", allTeams,
                "{all}", Game.getGame().getAllPlayers(),
                "{spectators}", UHCPlayers.countOf(UHCPlayer::isDead),
                "{border_size}", Game.getGame().getCurrentBorder(),
                "{border_size/2}", Game.getGame().getCurrentBorder() / 2,
                "{shrink_in}", LegacyFoundationAdapter.formatTime(Timers.getSecondsUntilEnable(Timers.BORDER)),
                "{game_time}", LegacyFoundationAdapter.formatTime(GameTimerRunnable.totalSecond));
    }
}
