package org.mcwonderland.uhc.command.impl.host;

import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.menu.impl.host.MainSettingsMenu;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public class InventoryEditorInputCommand implements CommandExecutor {

    public static final String FINISH_NAME = "finish";
    public static final String TO_HEAD_NAME = "tohead";

    private final String input;

    private InventoryEditorInputCommand(String input) {
        this.input = input;
    }

    public static void register(WonderlandUHC plugin) {
        PluginCommand finishCommand = plugin.getCommand(FINISH_NAME);

        if (finishCommand == null)
            throw new IllegalStateException("Command /" + FINISH_NAME + " is not declared in plugin.yml");

        finishCommand.setExecutor(new InventoryEditorInputCommand("finish"));

        PluginCommand toHeadCommand = plugin.getCommand(TO_HEAD_NAME);

        if (toHeadCommand == null)
            throw new IllegalStateException("Command /" + TO_HEAD_NAME + " is not declared in plugin.yml");

        toHeadCommand.setExecutor(new InventoryEditorInputCommand("tohead"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (MainSettingsMenu.handleInput(player, input))
            return true;

        Chat.send(player, "<red>目前沒有正在等待的設定輸入。</red>");
        return true;
    }
}
