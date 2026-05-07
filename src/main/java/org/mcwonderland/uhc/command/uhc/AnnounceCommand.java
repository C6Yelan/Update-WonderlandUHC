package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.model.broadcast.GameStartTimeInfo;
import org.mcwonderland.uhc.model.broadcast.impl.DiscordBroadcastSender;
import org.mineacademy.fo.command.SimpleSubCommand;

public class AnnounceCommand extends SimpleSubCommand {

    protected AnnounceCommand(UHCMainCommandGroup parent, String sublabel) {
        super(parent, sublabel);

        setUsage("<ip> <join_time> <start_time>");
        setMinArguments(3);
        setDescription("發送 Discord UHC 公告。");
        setPermission(UHCPermission.COMMAND_UHC_ANNOUNCE.toString());
    }

    @Override
    protected void onCommand() {
        DiscordBroadcastSender sender = new DiscordBroadcastSender();
        sender.getDependency().checkSoft();

        try {
            sender.sendBroadcast(new GameStartTimeInfo(args[0], args[1], args[2]));
            tell("&aDiscord 公告已送出。");
        } catch (RuntimeException e) {
            if (!LegacyFoundationAdapter.isFailure(e))
                throw e;

            tell(e.getMessage());
        }
    }
}
