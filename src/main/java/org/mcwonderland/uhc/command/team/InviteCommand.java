package org.mcwonderland.uhc.command.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.command.CommandHelper;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.util.Chat;
import org.bukkit.entity.Player;

class InviteCommand extends TeamOwnerCommand {

    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    protected InviteCommand(TeamCommandGroup parent, String sublabel) {
        super(parent, sublabel);

        setMinArguments(1);
        setUsage("<玩家>");
        setDescription("邀請其他人加入你的隊伍。");
        setPermission(UHCPermission.COMMAND_TEAM_INVITE.toString());
    }

    @Override
    protected void onOwnerCommand() {
        checkModeAndGameStatus();

        Player player = getPlayer();
        UHCTeam team = getTeam();
        UHCPlayer target = CommandHelper.findUHCPlayer(args[0]);

        checkInvitationValid(team, target);

        team.addInvited(target);
        team.sendMessage(PluginText.replaceToArray(
                CommandSettings.Team.Invite.INVITED,
                "{player}", player.getName(),
                "{target}", target.getName()));

        sendInvitation(target);
    }

    private void checkInvitationValid(UHCTeam team, UHCPlayer target) {
        checkExecuteSelf(target);

        if (target.getTeam() == team)
            returnTell(CommandSettings.Team.Invite.ALREADY_IN_YOUR_TEAM);

        checkFull(team);
    }

    private void sendInvitation(UHCPlayer uhcPlayer) {
        Player target = uhcPlayer.getPlayer();

        for (String s : CommandSettings.Team.Invite.INVITATION_MESSAGES) {
            if (s.contains("{click-join}")) {
                String clickHere = CommandSettings.Team.Invite.CLICK_HERE;
                target.sendMessage(runCommandComponent(
                        s.replace("{click-join}", clickHere),
                        "/team join " + getPlayer().getName(),
                        clickHere));
            } else
                Chat.send(target, s.replace("{player}", getPlayer().getName()));
        }
    }

    private Component runCommandComponent(String message, String command, String hover) {
        return LEGACY_AMPERSAND.deserialize(message)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(LEGACY_AMPERSAND.deserialize(hover)));
    }
}
