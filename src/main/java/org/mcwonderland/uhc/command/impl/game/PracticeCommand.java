package org.mcwonderland.uhc.command.impl.game;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.practice.Practice;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.util.Chat;

public class PracticeCommand implements CommandExecutor {

    public static final String NAME = "practice";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new PracticeCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_PRACTICE.checkPerms(player))
            return true;

        if (args.length > 0)
            return false;

        Practice practice = WonderlandUHC.getInstance().getPractice();

        if (practice.isInPractice(player))
            practice.quit(player);
        else
            practice.join(player);

        return true;
    }
}
