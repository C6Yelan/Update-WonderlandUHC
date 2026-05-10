package org.mcwonderland.uhc.command.impl.host;

import org.mineacademy.fo.command.SimpleCommand;

public class InventoryEditorInputCommand extends SimpleCommand {

    private final String input;

    public InventoryEditorInputCommand(String label, String input) {
        super(label);

        this.input = input;
    }

    @Override
    protected void onCommand() {
        checkConsole();

        if (!getPlayer().isConversing()) {
            tell("&c目前沒有正在等待的設定輸入。");
            return;
        }

        getPlayer().acceptConversationInput(input);
    }
}
