package org.mcwonderland.uhc.command.team;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.entity.Player;

import java.util.Locale;

class ListCommand extends TeamSubCommand {

    protected ListCommand(String sublabel) {
        super(sublabel);

        setUsage("[玩家]");
        setDescription("查看該玩家所處隊伍的資訊。");
        setPermission(UHCPermission.COMMAND_TEAM_LIST.toString());
    }

    @Override
    protected void onCommand() {
        if (args.length >= 1) {
            Player target = findPlayer(args[0]);
            sendTeamInfoAbout(target);
        } else
            sendTeamInfoAbout(getPlayer());
    }

    private void sendTeamInfoAbout(Player target) {
        UHCTeam targetTeam = UHCTeam.getTeam(target);
        checkNotNull(targetTeam, CommandSettings.Team.PLAYER_HAS_NO_TEAM);

        for (String msg : CommandSettings.TeamList.MESSAGES) {
            if (msg.contains("{playeralive}")) {
                for (UHCPlayer uhcPlayer : targetTeam.getAlives())
                    tell(PluginText.replaceToString(
                            msg,
                            "{playeralive}", uhcPlayer.getName(),
                            "{heal}", Extra.formatHealth(uhcPlayer.getEntity().getHealth())));
            } else if (msg.contains("{playerdeath}")) {
                for (UHCPlayer uhcPlayer : targetTeam.getPlayers())
                    if (uhcPlayer.isDead())
                        tell(PluginText.replaceToString(msg, "{playerdeath}", uhcPlayer.getName()));
            } else
                tell(PluginText.replaceToString(
                                msg,
                                "{character}", targetTeam.getSymbol(),
                                "{teamname}", targetTeam.getName())
                        .replace("{color}", miniMessageColorTag(targetTeam)));
        }

        Extra.sound(getPlayer(), Sounds.Commands.TEAM_INFO);
    }

    private String miniMessageColorTag(UHCTeam team) {
        return "<" + team.getColor().name().toLowerCase(Locale.ROOT) + ">";
    }

    @Override
    boolean completesPlayerName() {
        return true;
    }
}
