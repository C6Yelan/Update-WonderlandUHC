package org.mcwonderland.uhc.command.team;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.api.enums.TeamSplitMode;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.GameUtils;

import java.util.Arrays;
import java.util.List;

public abstract class TeamSubCommand {

    private static final String PLAYER_NOT_ONLINE = "&cPlayer {player} &cis not online on this server.";

    private final String sublabel;
    private String usage = "";
    private String description = "";
    private String permission;
    private int minArguments;

    protected CommandSender sender;
    protected Player player;
    protected String label;
    protected String[] args = new String[0];

    protected TeamSubCommand(String sublabel) {
        this.sublabel = sublabel;
    }

    final boolean execute(CommandSender sender, String label, String[] args) {
        this.sender = sender;
        this.label = label;
        this.args = args;

        if (!(sender instanceof Player commandPlayer)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        this.player = commandPlayer;

        if (!hasPermission(sender)) {
            Chat.send(sender, Messages.NO_PERMISSION.replace("{permission}", permission));
            return true;
        }

        if (args.length < minArguments) {
            tellUsage();
            return true;
        }

        try {
            onCommand();
        } catch (TeamCommandFailure failure) {
            Chat.send(sender, failure.getMessages());
        }

        return true;
    }

    protected abstract void onCommand();

    String getSublabel() {
        return sublabel;
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

    boolean completesPlayerName() {
        return false;
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

    protected final Player getPlayer() {
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
        throw new TeamCommandFailure(message);
    }

    protected final void checkBoolean(boolean value, String falseMessage) {
        if (!value)
            returnTell(falseMessage);
    }

    protected final void checkNotNull(Object value, String messageIfNull) {
        if (value == null)
            returnTell(messageIfNull);
    }

    protected final Player findPlayer(String name) {
        Player found = PluginPlayers.getByName(name, true);
        checkNotNull(found, PLAYER_NOT_ONLINE.replace("{player}", name));
        return found;
    }

    protected final UHCPlayer findUHCPlayer(String name) {
        Player found = PluginPlayers.getByName(name, false);
        checkNotNull(found, PLAYER_NOT_ONLINE.replace("{player}", name));
        return UHCPlayer.getUHCPlayer(found);
    }

    protected final String joinArgs(int from, int to) {
        return String.join(" ", Arrays.copyOfRange(args, from, to));
    }

    private void tellUsage() {
        Chat.send(sender, CommandSettings.LABEL_USAGE);
        Chat.send(sender, "&c/" + label + " " + sublabel + getUsageSuffix());
    }

    protected final UHCTeam getTeam() {
        return getTeam(getUhcPlayer());
    }

    protected final UHCPlayer getUhcPlayer(){
        return UHCPlayer.getUHCPlayer(getPlayer());
    }

    protected final UHCTeam getTeam(UHCPlayer uhcPlayer) {
        checkBoolean(uhcPlayer.getTeam() != null,
                uhcPlayer == getUhcPlayer() ?
                        CommandSettings.Team.YOU_DONT_HAVE_TEAM :
                        CommandSettings.Team.PLAYER_HAS_NO_TEAM
        );

        return uhcPlayer.getTeam();
    }

    protected final void checkChosenMode() {
        if (Game.getSettings().getTeamSettings().getTeamSplitMode() != TeamSplitMode.CHOSEN)
            returnTell(Messages.Team.ONLY_IN_CHOSEN_MODE);
    }

    protected final void checkModeAndGameStatus() {
        checkChosenMode();
        if (!GameUtils.isWaiting())
            returnTell(Messages.USE_ONLY_WHILE_WAITING);
    }

    protected final void checkWaiting() {
        if (!GameUtils.isWaiting())
            returnTell(Messages.USE_ONLY_WHILE_WAITING);
    }

    protected final void checkExecuteSelf(UHCPlayer target) {
        checkBoolean(getPlayer() != target.getPlayer(), CommandSettings.CANT_EXECUTE_SELF);
    }

    protected final void checkAlreadyInTeam() {
        checkBoolean(getUhcPlayer().getTeam() == null, CommandSettings.Team.ALREADY_HAS_ONE);
    }

    protected final void checkInTeam(UHCPlayer target) {
        checkBoolean(getTeam() == target.getTeam(), CommandSettings.Team.PLAYER_NOT_IN_TEAM);
    }

    protected final void checkFull(UHCTeam uhcTeam) {
        checkBoolean(!uhcTeam.isFull(), Messages.Team.FULL_MSG);
    }

    private static final class TeamCommandFailure extends RuntimeException {

        private final String[] messages;

        private TeamCommandFailure(String... messages) {
            this.messages = messages;
        }

        private String[] getMessages() {
            return messages;
        }
    }
}
