package org.mcwonderland.uhc.command.impl.host;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.application.border.BorderShrinkRequestService;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.GameUtils;

/**
 * 2019-11-27 下午 01:05
 */
public class BorderCommand implements CommandExecutor {

    public static final String NAME = "border";

    private final BorderShrinkRequestService borderShrinkRequest = new BorderShrinkRequestService();

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        command.setExecutor(new BorderCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_BORDER.checkPerms(player))
            return true;

        if (!GameUtils.isGameStarted()) {
            Chat.send(player, Messages.NOT_YET_STARTED);
            return true;
        }

        int max = borderShrinkRequest.maxRequestedSize();
        Integer size = parseRequestedSize(player, args, max);

        if (size == null)
            return true;

        borderShrinkRequest.requestShrink(size);
        return true;
    }

    private Integer parseRequestedSize(Player player, String[] args, int max) {
        if (args.length < 1) {
            Chat.send(player, numberRangeMessage(max));
            return null;
        }

        try {
            int size = Integer.parseInt(args[0]);

            if (size >= 1 && size <= max)
                return size;
        } catch (NumberFormatException ignored) {
        }

        Chat.send(player, numberRangeMessage(max));
        return null;
    }

    private String numberRangeMessage(int max) {
        return CommandSettings.Border.ONLY_NUMBER_BETWEEN
                .replace("{min}", "1")
                .replace("{max}", String.valueOf(max));
    }
}
