package org.mcwonderland.uhc.command.impl.game;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.SpectateMode;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.GameUtils;

/**
 * 2019-11-24 下午 12:50
 */
public class SpecToggleCommand implements CommandExecutor {

    public static final String NAME = "spectoggle";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new SpecToggleCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_SPECTOGGLE.checkPerms(player))
            return true;

        if (!GameUtils.isGameStarted()) {
            Chat.send(player, Messages.NOT_YET_STARTED);
            return true;
        }

        if (GameUtils.isGamingPlayer(player)) {
            Chat.send(player, Messages.ONLY_FOR_SPECTATOR);
            return true;
        }

        if (Settings.Spectator.SPECTATE_MODE != SpectateMode.DEFAULT) {
            Chat.send(player, CommandSettings.Uhc.SpecToggle.ONLY_FOR_DEFAULT_SPECTATE_MODE);
            return true;
        }

        GameMode gameMode = player.getGameMode();
        player.setGameMode(gameMode == GameMode.CREATIVE ? GameMode.SPECTATOR : GameMode.CREATIVE);

        Chat.send(player, CommandSettings.Uhc.SpecToggle.GAMEMODE_TOGGLED.replace("{cmd}", label));
        Extra.sound(player, Sounds.Commands.SPEC_TOGGLE);
        return true;
    }

}
