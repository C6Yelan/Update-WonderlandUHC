package org.mcwonderland.uhc.command.impl.info;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.model.GamePlaceholderReplacer;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

/**
 * 2019-12-02 下午 03:30
 */
public class ConfigCommand implements CommandExecutor {

    public static final String NAME = "config";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new ConfigCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String permission = UHCPermission.COMMAND_CONFIG.toString();

        if (!sender.hasPermission(permission)) {
            Chat.send(sender, Messages.NO_PERMISSION.replace("{permission}", permission));
            return true;
        }

        Chat.send(sender, GamePlaceholderReplacer.replace(CommandSettings.Config.MESSAGES));
        return true;
    }
}
