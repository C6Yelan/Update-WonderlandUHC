package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.menu.MainGui;

/**
 * 2019-11-24 下午 01:15
 */
public class EditCommand extends UHCSubCommand {

    protected EditCommand(String subLabel) {
        super(subLabel);

        setDescription("開啟設定介面。");
        setPermission(UHCPermission.COMMAND_UHC_EDIT.toString());
        setPlayerOnly(true);
    }

    @Override
    protected void onCommand() {
        MainGui.abrirMenu(getPlayer());
    }
}
