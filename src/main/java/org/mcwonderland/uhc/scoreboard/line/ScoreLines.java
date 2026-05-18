package org.mcwonderland.uhc.scoreboard.line;

import org.mcwonderland.uhc.platform.text.PluginText;

import org.mcwonderland.uhc.game.player.UHCPlayer;

import java.util.List;

public abstract class ScoreLines {

    private final List<String> lines;
    private List<String> currentGlobalReplacedLines;

    public ScoreLines(List<String> lines) {
        this.lines = lines;
    }

    public List<String> getFor(UHCPlayer uhcPlayer) {
        return replace(uhcPlayer, currentGlobalReplacedLines);
    }

    public void updateGlobalVariables() {
        currentGlobalReplacedLines = replaceGlobal(lines);
    }

    protected List<String> replace(UHCPlayer uhcPlayer, List<String> lines) {
        return lines;
    }

    protected List<String> replaceGlobal(List<String> lines) {
        return lines;
    }

    protected final List<String> replaceLines(List<String> lines, Object... replacements) {
        return PluginText.replaceToList(lines, replacements);
    }
}
