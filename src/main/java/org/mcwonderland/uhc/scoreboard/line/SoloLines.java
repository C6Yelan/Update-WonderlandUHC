package org.mcwonderland.uhc.scoreboard.line;

import org.mcwonderland.uhc.game.player.UHCPlayer;

import java.util.List;

public class SoloLines extends GameLines {

    public SoloLines(List<String> lines) {
        super(lines);
    }

    @Override
    protected List<String> replace(UHCPlayer uhcPlayer, List<String> lines) {
        return replaceLines(super.replace(uhcPlayer, lines), "{kills}", uhcPlayer.getStats().kills);
    }
}
