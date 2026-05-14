package org.mcwonderland.uhc.command.impl.host;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.application.border.BorderShrinkRequestService;
import org.mcwonderland.uhc.command.CommandHelper;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mineacademy.fo.command.SimpleCommand;

/**
 * 2019-11-27 下午 01:05
 */
public class BorderCommand extends SimpleCommand {

    private final BorderShrinkRequestService borderShrinkRequest = new BorderShrinkRequestService();

    public BorderCommand(String label) {
        super(label);

        setMinArguments(1);
        setUsage("<大小>");
        setDescription("強制收縮邊界。");
        setPermission(UHCPermission.COMMAND_BORDER.toString());
    }

    @Override
    protected void onCommand() {
        CommandHelper.checkGameStarted();

        int size = findNumber(0, 1, borderShrinkRequest.maxRequestedSize(), CommandSettings.Border.ONLY_NUMBER_BETWEEN);
        borderShrinkRequest.requestShrink(size);
    }
}
