package org.mcwonderland.uhc.command.uhc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.mcwonderland.uhc.References;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.util.Chat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UHCCommand implements CommandExecutor, TabCompleter {

    public static final String NAME = "uhc";

    private final WonderlandUHC plugin;
    private final Map<String, UHCSubCommand> subcommands = new LinkedHashMap<>();

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        UHCCommand executor = new UHCCommand(plugin);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private UHCCommand(WonderlandUHC plugin) {
        this.plugin = plugin;

        register(new ReloadCommand("reload|rl"));
        register(new ChooseWorldCommand("choose"));
        register(new EditCommand("edit"));
        register(new RegenWorldCommand("regen"));
        register(new ResetTeamCommand("resetteam"));
        register(new SetHostCommand("sethost"));
        register(new SplitTeamCommand("splitteam"));
        register(new SwitchTeamCommand("switchteam"));
        register(new StopCommand("stop"));
        register(new TpUHCWorldCommand("tp"));
        register(new StartCommand("start"));
        register(new TutorialCommand("tutorial"));
    }

    private void register(UHCSubCommand subcommand) {
        for (String label : subcommand.getLabels())
            subcommands.put(label, subcommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendNoArgsHeader(sender);
            return true;
        }

        if (isHelp(args[0])) {
            sendHelp(sender, label);
            return true;
        }

        UHCSubCommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));

        if (subcommand == null) {
            sendHelp(sender, label);
            return true;
        }

        String[] subcommandArgs = java.util.Arrays.copyOfRange(args, 1, args.length);
        return subcommand.execute(sender, label, args[0], subcommandArgs);
    }

    private boolean isHelp(String arg) {
        return "?".equals(arg) || "help".equalsIgnoreCase(arg);
    }

    private void sendNoArgsHeader(CommandSender sender) {
        Chat.send(sender,
                "<dark_gray><strikethrough>-----------------------------------------------------</strikethrough></dark_gray>",
                "<white>  WonderlandUHC </white><gray>" + plugin.getPluginMeta().getVersion() + "</gray>",
                " ",
                "<gray>插件下載頁面: </gray><aqua>" + References.DOWNLOAD_LINK + "</aqua>",
                "<dark_gray><strikethrough>-----------------------------------------------------</strikethrough></dark_gray>");
    }

    private void sendHelp(CommandSender sender, String label) {
        Chat.send(sender, "<dark_gray><strikethrough>-----------------------------------------------------</strikethrough></dark_gray>");

        for (UHCSubCommand subcommand : uniqueSubcommands()) {
            if (!subcommand.hasPermission(sender))
                continue;

            Chat.send(sender, "<gold>/" + label + " " + subcommand.getPrimaryLabel()
                    + subcommand.getUsageSuffix()
                    + "</gold> <yellow>- " + subcommand.getDescription() + "</yellow>");
        }

        Chat.send(sender, "<dark_gray><strikethrough>-----------------------------------------------------</strikethrough></dark_gray>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return completeSubcommands(sender, args[0]);

        UHCSubCommand subcommand = subcommands.get(args[0].toLowerCase(Locale.ROOT));

        if (subcommand == null || !subcommand.hasPermission(sender))
            return List.of();

        return subcommand.tabComplete(java.util.Arrays.copyOfRange(args, 1, args.length));
    }

    private List<String> completeSubcommands(CommandSender sender, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (Map.Entry<String, UHCSubCommand> entry : subcommands.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue().hasPermission(sender))
                completions.add(entry.getKey());
        }

        return completions;
    }

    private List<UHCSubCommand> uniqueSubcommands() {
        List<UHCSubCommand> unique = new ArrayList<>();

        for (UHCSubCommand subcommand : subcommands.values()) {
            if (!unique.contains(subcommand))
                unique.add(subcommand);
        }

        return unique;
    }
}
