package org.mcwonderland.uhc.command.impl.info;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayers;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.GameUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.util.Iterator;

/**
 * 2019-11-27 下午 01:05
 */
public class TopKillsCommand implements CommandExecutor {

    public static final String NAME = "topkills";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new TopKillsCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_TOPKILLS.checkPerms(player))
            return true;

        if (!GameUtils.isGameStarted()) {
            Chat.send(player, Messages.NOT_YET_STARTED);
            return true;
        }

        Iterator<UHCPlayer> topKills = UHCPlayers.stream()
                .sorted((o1, o2) -> Integer.compare(o2.getStats().kills, o1.getStats().kills))
                .iterator();
        int place = 0;

        for (String msg : CommandSettings.TopKills.MESSAGES) {
            if (msg.contains("{player}")) {
                if (topKills.hasNext()) {
                    UHCPlayer uhcPlayer = topKills.next();
                    Chat.send(player, msg.replace("{player}", uhcPlayer.getName())
                            .replace("{number}", ++place + "")
                            .replace("{kills}", uhcPlayer.getStats().kills + ""));
                }
            } else
                Chat.send(player, msg);
        }

        Extra.sound(player, Sounds.Commands.TOP_KILLS);
        return true;
    }
}
