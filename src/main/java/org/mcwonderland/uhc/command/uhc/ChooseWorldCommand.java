package org.mcwonderland.uhc.command.uhc;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.application.world.PreviewWorldSelectionService;
import org.mineacademy.fo.command.SimpleSubCommand;

/**
 * 2019-11-24 下午 12:50
 */
public class ChooseWorldCommand extends SimpleSubCommand {

    private final PreviewWorldSelectionService previewWorldSelection = new PreviewWorldSelectionService();

    protected ChooseWorldCommand(UHCMainCommandGroup parent, String subLabel) {
        super(parent, subLabel);

        setMinArguments(0);
        setDescription("選擇此遊戲世界並開始載入地圖。");
        setPermission(UHCPermission.COMMAND_UHC_CHOOSE.toString());
    }

    @Override
    protected void onCommand() {
        previewWorldSelection.select(isPlayer() ? getPlayer() : null);
    }
}
