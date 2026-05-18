package org.mcwonderland.uhc.command.impl.info;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.menu.impl.game.StatsMenu;
import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.util.Chat;

/**
 * 2019-11-27 下午 01:05
 */
public class StatsCommand implements CommandExecutor {

    public static final String NAME = "stats";
    private static final String PLAYER_NOT_ONLINE = "&cPlayer {player} &cis not online on this server.";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new StatsCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_STATS.checkPerms(viewer))
            return true;

        if (args.length >= 1)
            return openStats(viewer, args[0]);

        new StatsMenu(viewer).displayTo(viewer);
        return true;
    }

    private boolean openStats(Player viewer, String targetName) {
        Player target = PluginPlayers.getByName(targetName, true);

        if (target == null) {
            Chat.send(viewer, PLAYER_NOT_ONLINE.replace("{player}", targetName));
            return true;
        }

        new StatsMenu(target).displayTo(viewer);
        return true;
    }
}
