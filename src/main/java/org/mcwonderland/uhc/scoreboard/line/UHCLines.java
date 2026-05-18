package org.mcwonderland.uhc.scoreboard.line;

import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;

import java.util.List;

public abstract class UHCLines extends ScoreLines {

    public UHCLines(List<String> lines) {
        super(lines);
    }

    @Override
    protected List<String> replace(UHCPlayer uhcPlayer, List<String> lines) {
        return replaceLines(lines, "{ign}", uhcPlayer.getName());
    }

    @Override
    protected List<String> replaceGlobal(List<String> lines) {
        return replaceLines(lines,
                "{online_players}", PluginPlayers.onlinePlayers().size(),
                "{host}", Game.getGame().getHost());
    }
}
