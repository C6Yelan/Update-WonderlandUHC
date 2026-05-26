package org.mcwonderland.uhc.command.impl.game;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.Dependency;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.hook.voice.DiscordVoiceHook;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.Chat;

public class ReconnectCommand implements CommandExecutor {

    public static final String NAME = "reconnect";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new ReconnectCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_RECONNECT.checkPerms(player))
            return true;

        if (!Dependency.DISCORD_SRV.isHooked()) {
            Chat.send(player, PluginText.replaceToString(
                    Messages.Dependency.REQUIRE_SOFT_DEPENDENCY,
                    "{plugin}", Dependency.DISCORD_SRV.getPluginName(),
                    "{url}", PluginText.formatted(Dependency.DISCORD_SRV.getClickableDownloadUrlTag())
            ));
            return true;
        }

        if (!Settings.DiscordVoice.USE) {
            Chat.send(player, Messages.DiscordVoice.MOVE_FAILED);
            return true;
        }

        Chat.send(player, Messages.DiscordVoice.RECONNECTING);
        DiscordVoiceHook.reconnect(UHCPlayer.getUHCPlayer(player));
        return true;
    }
}
