package org.mcwonderland.uhc.command.impl.host;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.settings.spawn.Spawns;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

/**
 * 2019-11-27 下午 01:05
 */
public class SetSpawnCommand implements CommandExecutor {

    public static final String NAME = "setspawn";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new SetSpawnCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_SET_SPAWN.checkPerms(player))
            return true;

        Spawns.getLobbySpawn().updateLocation(player.getLocation());
        Chat.send(player, CommandSettings.SetSpawn.SPAWN_SAVED);
        Extra.sound(player, Sounds.Commands.SET_SPAWN);
        return true;
    }
}
