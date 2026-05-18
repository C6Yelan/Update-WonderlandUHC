package org.mcwonderland.uhc.command.impl.game;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.GameUtils;

/**
 * 2019-11-27 下午 01:05
 */
public class SendCoordsCommand implements CommandExecutor {

    public static final String NAME = "sendcoords";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new SendCoordsCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_SENDCOORDS.checkPerms(player))
            return true;

        if (!GameUtils.isGameStarted()) {
            Chat.send(player, Messages.NOT_YET_STARTED);
            return true;
        }

        if (!GameUtils.isGamingPlayer(player)) {
            Chat.send(player, Messages.NOT_GAMING_PLAYER);
            return true;
        }

        UHCTeam team = UHCTeam.getTeam(player);
        Location location = player.getLocation();

        team.sendMessage(PluginText.replaceToString(
                CommandSettings.SendCoords.FORMAT,
                "{teamname}", team.getName(),
                "{color}", team.getColor(),
                "{player}", player.getName(),
                "{x}", location.getBlockX(),
                "{y}", location.getBlockY(),
                "{z}", location.getBlockZ()));

        Extra.sound(team.getAlivePlayers(), Sounds.Commands.SEND_COORDS);
        return true;
    }
}
