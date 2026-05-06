package org.mcwonderland.uhc.scoreboard.line;

import org.mcwonderland.uhc.util.RuntimeUtil;

import java.util.List;

public class StaffLines extends GameLines {

    public StaffLines(List<String> lines) {
        super(lines);
    }

    @Override
    protected List<String> replaceGlobal(List<String> lines) {
        return replaceLines(super.replaceGlobal(lines),
                "{tps}", RuntimeUtil.getTPSFormat().format(RuntimeUtil.getTPS(0)),
                "{free_ram}", RuntimeUtil.AvailableMemory());
    }
}
