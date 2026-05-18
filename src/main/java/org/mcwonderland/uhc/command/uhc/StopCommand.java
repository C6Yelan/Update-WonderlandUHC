package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.application.match.MatchStopService;

/**
 * 2019-11-24 下午 12:50
 */
public class StopCommand extends UHCSubCommand {

    private final MatchStopService matchStop = new MatchStopService();

    protected StopCommand(String subLabel) {
        super(subLabel);

        setDescription("關閉伺服器並移除本場遊戲的相關資料。");
        setPermission(UHCPermission.COMMAND_UHC_STOP.toString());
    }

    @Override
    protected void onCommand() {
        matchStop.stopServer();
    }

}
