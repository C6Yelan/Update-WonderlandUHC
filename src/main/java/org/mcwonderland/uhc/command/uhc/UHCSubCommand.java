package org.mcwonderland.uhc.command.uhc;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.GameUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

abstract class UHCSubCommand {

    private static final String PLAYER_NOT_ONLINE = "<red>Player {player} is not online on this server.</red>";

    private final List<String> labels;
    private String usage = "";
    private String description = "";
    private String permission;
    private int minArguments;
    private boolean playerOnly;

    protected CommandSender sender;
    protected Player player;
    protected String label;
    protected String sublabel;
    protected String[] args = new String[0];

    protected UHCSubCommand(String labels) {
        this.labels = Arrays.stream(labels.split("(\\||/)"))
                .map(label -> label.toLowerCase(Locale.ROOT))
                .toList();
    }

    final boolean execute(CommandSender sender, String label, String sublabel, String[] args) {
        this.sender = sender;
        this.label = label;
        this.sublabel = sublabel;
        this.args = args;
        this.player = sender instanceof Player commandPlayer ? commandPlayer : null;

        if (!hasPermission(sender)) {
            Chat.send(sender, Messages.NO_PERMISSION.replace("{permission}", permission));
            return true;
        }

        if (playerOnly && player == null) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (args.length < minArguments) {
            tellUsage();
            return true;
        }

        try {
            onCommand();
        } catch (UHCCommandFailure failure) {
            Chat.send(sender, failure.getMessages());
        }

        return true;
    }

    protected abstract void onCommand();

    List<String> getLabels() {
        return labels;
    }

    String getPrimaryLabel() {
        return labels.get(0);
    }

    String getUsageSuffix() {
        return usage.isBlank() ? "" : " " + usage;
    }

    String getDescription() {
        return description;
    }

    boolean hasPermission(CommandSender sender) {
        return permission == null || sender.hasPermission(permission);
    }

    List<String> tabComplete(String[] args) {
        return List.of();
    }

    protected final void setUsage(String usage) {
        this.usage = usage;
    }

    protected final void setDescription(String description) {
        this.description = description;
    }

    protected final void setPermission(String permission) {
        this.permission = permission;
    }

    protected final void setMinArguments(int minArguments) {
        this.minArguments = minArguments;
    }

    protected final void setPlayerOnly(boolean playerOnly) {
        this.playerOnly = playerOnly;
    }

    protected final boolean isPlayer() {
        return player != null;
    }

    protected final Player getPlayer() {
        if (player == null)
            returnTell(CommandSettings.NO_CONSOLE);

        return player;
    }

    protected final String getLabel() {
        return label;
    }

    protected final void tell(String... messages) {
        Chat.send(sender, messages);
    }

    protected final void tell(List<String> messages) {
        Chat.send(sender, messages);
    }

    protected final void returnTell(String message) {
        throw new UHCCommandFailure(message);
    }

    protected final void checkWaiting() {
        if (!GameUtils.isWaiting())
            returnTell(Messages.USE_ONLY_WHILE_WAITING);
    }

    protected final UHCPlayer findUHCPlayer(String name) {
        Player found = PluginPlayers.getByName(name, false);

        if (found == null)
            returnTell(PLAYER_NOT_ONLINE.replace("{player}", name));

        return UHCPlayer.getUHCPlayer(found);
    }

    protected final List<String> completeLastWord(String[] args, String... suggestions) {
        if (args.length == 0)
            return List.of(suggestions);

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(prefix))
                completions.add(suggestion);
        }

        return completions;
    }

    protected final List<String> completePlayerNames(String[] args) {
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (String name : PluginPlayers.playerNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix))
                completions.add(name);
        }

        return completions;
    }

    private void tellUsage() {
        Chat.send(sender, CommandSettings.LABEL_USAGE);
        Chat.send(sender, "<red>/" + label + " " + getPrimaryLabel() + getUsageSuffix() + "</red>");
    }

    private static final class UHCCommandFailure extends RuntimeException {

        private final String[] messages;

        private UHCCommandFailure(String... messages) {
            this.messages = messages;
        }

        private String[] getMessages() {
            return messages;
        }
    }
}
