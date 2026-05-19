package org.mcwonderland.uhc.command.impl.info;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;

/**
 * 2019-11-27 下午 01:05
 */
public class ViewHealCommand implements CommandExecutor {

    public static final String NAME = "viewheal";

    private static final String PLAYER_NOT_ONLINE = "<red>Player {player} is not online on this server.</red>";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new ViewHealCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String permission = UHCPermission.COMMAND_VIEWHEAL.toString();

        if (!sender.hasPermission(permission)) {
            Chat.send(sender, Messages.NO_PERMISSION.replace("{permission}", permission));
            return true;
        }

        if (args.length < 1)
            return false;

        Player target = PluginPlayers.getByName(args[0], true);

        if (target == null) {
            Chat.send(sender, PLAYER_NOT_ONLINE.replace("{player}", args[0]));
            return true;
        }

        Chat.send(sender, CommandSettings.Heal.FORMAT
                .replace("{player}", target.getName())
                .replace("{heal}", "" + Extra.formatHealth(target.getHealth())));
        return true;
    }
}
