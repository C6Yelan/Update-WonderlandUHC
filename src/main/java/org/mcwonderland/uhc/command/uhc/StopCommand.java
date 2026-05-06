package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.Bukkit;
import org.mineacademy.fo.command.SimpleSubCommand;

/**
 * 2019-11-24 下午 12:50
 */
public class StopCommand extends SimpleSubCommand {

    protected StopCommand(UHCMainCommandGroup parent, String subLabel) {
        super(parent, subLabel);

        setDescription("關閉伺服器並移除本場遊戲的相關資料。");
        setPermission(UHCPermission.COMMAND_UHC_STOP.toString());
    }

    @Override
    protected void onCommand() {
        CacheSaver.deleteCache();
        LegacyFoundationAdapter.getOnlinePlayers().forEach(Extra::sendToFallbackServer);
        Bukkit.shutdown();
    }

}
