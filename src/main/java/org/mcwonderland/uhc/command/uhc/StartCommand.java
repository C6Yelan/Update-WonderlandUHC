package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.application.match.MatchStartRequestService;
import org.mineacademy.fo.command.SimpleSubCommand;

public class StartCommand extends SimpleSubCommand {

    private final MatchStartRequestService matchStartRequest = new MatchStartRequestService();

    protected StartCommand(UHCMainCommandGroup parent, String sublabel) {
        super(parent, sublabel);

        setUsage("<config|host>");
        setDescription("開始遊戲！");
        setPermission(UHCPermission.COMMAND_UHC_START.toString());
    }

    @Override
    protected void onCommand() {
        matchStartRequest.requestStart(sender);
    }
}
