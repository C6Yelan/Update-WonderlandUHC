package org.mcwonderland.uhc.command.impl.info;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.menu.impl.game.EnabledScenariosMenu;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

/**
 * 2019-11-27 上午 10:32
 */
public class ScenariosCommand implements CommandExecutor {

    public static final String NAME = "scenarios";

    private final WonderlandUHC plugin;

    private ScenariosCommand(WonderlandUHC plugin) {
        this.plugin = plugin;
    }

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new ScenariosCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_SCENARIOS.checkPerms(player))
            return true;

        new EnabledScenariosMenu(plugin.getScenarioManager()).displayTo(player);
        return true;
    }
}
