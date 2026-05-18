package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.application.match.MatchStartRequestService;

public class StartCommand extends UHCSubCommand {

    private final MatchStartRequestService matchStartRequest = new MatchStartRequestService();

    protected StartCommand(String sublabel) {
        super(sublabel);

        setUsage("<config|host>");
        setDescription("開始遊戲！");
        setPermission(UHCPermission.COMMAND_UHC_START.toString());
    }

    @Override
    protected void onCommand() {
        matchStartRequest.requestStart(sender);
    }
}
