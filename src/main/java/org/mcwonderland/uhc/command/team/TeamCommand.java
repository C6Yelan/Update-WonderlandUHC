package org.mcwonderland.uhc.command.team;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.util.Chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeamCommand implements CommandExecutor, TabCompleter {

    public static final String NAME = "team";

    private final Map<String, TeamSubCommand> subcommands = new LinkedHashMap<>();

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        TeamCommand executor = new TeamCommand();
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private TeamCommand() {
        register(new ChatCommand("chat"));
        register(new CreateCommand("create"));
        register(new DisbandCommand("disband"));
        register(new InviteCommand("invite"));
        register(new JoinCommand("join"));
        register(new LeaveCommand("leave"));
        register(new KickCommand("kick"));
        register(new ListCommand("list"));
        register(new PromoteCommand("promote"));
        register(new PublicCommand("public"));
        register(new SettingsCommand("settings"));
    }

    private void register(TeamSubCommand subcommand) {
        subcommands.put(subcommand.getSublabel(), subcommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || isHelp(args[0])) {
            sendHelp(sender, label);
            return true;
        }

        TeamSubCommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));

        if (subcommand == null) {
            Chat.send(sender, CommandSettings.INVALID_ARGUMENT.replace("{label}", label));
            return true;
        }

        String[] subcommandArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
        return subcommand.execute(sender, label, subcommandArgs);
    }

    private boolean isHelp(String arg) {
        return "?".equals(arg) || "help".equalsIgnoreCase(arg);
    }

    private void sendHelp(CommandSender sender, String label) {
        Chat.send(sender, "<dark_gray><strikethrough>-----------------------------------------------------</strikethrough></dark_gray>");

        for (TeamSubCommand subcommand : subcommands.values()) {
            if (!subcommand.hasPermission(sender))
                continue;

            Chat.send(sender, "<gold>/" + label + " " + subcommand.getSublabel()
                    + subcommand.getUsageSuffix()
                    + "</gold> <yellow>- " + subcommand.getDescription() + "</yellow>");
        }

        Chat.send(sender, "<dark_gray><strikethrough>-----------------------------------------------------</strikethrough></dark_gray>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return completeSubcommands(sender, args[0]);

        if (args.length == 2) {
            TeamSubCommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));

            if (subcommand != null && subcommand.completesPlayerName())
                return completePlayerNames(args[1]);
        }

        return List.of();
    }

    private List<String> completeSubcommands(CommandSender sender, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (TeamSubCommand subcommand : subcommands.values()) {
            if (subcommand.hasPermission(sender) && subcommand.getSublabel().startsWith(prefix))
                completions.add(subcommand.getSublabel());
        }

        return completions;
    }

    private List<String> completePlayerNames(String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (String name : PluginPlayers.playerNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix))
                completions.add(name);
        }

        return completions;
    }
}
