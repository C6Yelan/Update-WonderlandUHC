package org.mcwonderland.uhc.command.impl.host.whitelist;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.platform.player.PlayerCollection;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WhitelistCommand implements CommandExecutor, TabCompleter {

    public static final String NAME = "whitelist";

    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "list", "clear");

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        WhitelistCommand executor = new WhitelistCommand();
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(UHCPermission.COMMAND_WHITELIST.toString())) {
            Chat.send(sender, Messages.NO_PERMISSION.replace("{permission}", UHCPermission.COMMAND_WHITELIST.toString()));
            return true;
        }

        if (args.length < 1)
            return false;

        String subcommand = args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {
            case "add":
                return add(sender, args);
            case "remove":
                return remove(sender, args);
            case "list":
                return list(sender);
            case "clear":
                return clear(sender);
            default:
                return false;
        }
    }

    private boolean add(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Chat.send(sender, CommandSettings.Whitelist.NAME_REQUIRED);
            return true;
        }

        for (int i = 1; i < args.length; i++)
            add(sender, args[i]);

        return true;
    }

    private void add(CommandSender sender, String name) {
        if (getWhitelist().contains(name)) {
            Chat.send(sender, CommandSettings.Whitelist.ALREADY_ADDED.replace("{player}", name));
            return;
        }

        getWhitelist().add(name);
        Chat.broadcastWithPerm(UHCPermission.COMMAND_WHITELIST.toString(), CommandSettings.Whitelist.ADDED
                .replace("{player}", name)
                .replace("{op}", sender.getName()));
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Chat.send(sender, CommandSettings.Whitelist.NAME_REQUIRED);
            return true;
        }

        String toRemove = args[1];

        if (!getWhitelist().contains(toRemove)) {
            Chat.send(sender, CommandSettings.Whitelist.ALREADY_REMOVED.replace("{player}", toRemove));
            return true;
        }

        getWhitelist().remove(toRemove);
        Chat.broadcastWithPerm(UHCPermission.COMMAND_WHITELIST.toString(), CommandSettings.Whitelist.REMOVED
                .replace("{player}", toRemove)
                .replace("{op}", sender.getName()));
        return true;
    }

    private boolean list(CommandSender sender) {
        Chat.send(sender, PluginText.replaceToList(
                CommandSettings.Whitelist.LIST,
                "{players}", getWhitelist()));
        return true;
    }

    private boolean clear(CommandSender sender) {
        getWhitelist().clear();
        Chat.broadcastWithPerm(UHCPermission.COMMAND_WHITELIST.toString(), CommandSettings.Whitelist.CLEARED
                .replace("{player}", sender.getName()));
        return true;
    }

    private PlayerCollection getWhitelist() {
        return Game.getGame().getWhiteList();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1)
            return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (String subcommand : SUBCOMMANDS) {
            if (subcommand.startsWith(prefix))
                completions.add(subcommand);
        }

        return completions;
    }
}
