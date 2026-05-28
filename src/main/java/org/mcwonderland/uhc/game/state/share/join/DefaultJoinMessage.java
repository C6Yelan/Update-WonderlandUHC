package org.mcwonderland.uhc.game.state.share.join;

import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.entity.Player;

public class DefaultJoinMessage implements JoinBehavior {

    @Override
    public void onJoin(UHCJoinEvent e) {

        Player player = e.getPlayer();
        Game game = e.getGame();

        if (WorldLoadingCacheState.getLoadingStatus() == LoadingStatus.DONE) {
            Chat.send(player, PluginText.replaceToArray(
                    Messages.Lobby.WELCOME_MSG,
                    "{player}", player.getName(),
                    "{host}", game.getHost(),
                    "{title}", Game.getSettings().getTitle()));

            Chat.broadcast(Messages.Lobby.PLAYER_JOIN_MSG
                    .replace("{player}", player.getName())
                    .replace("{online}", "" + Extra.getOnlinePlayers())
                    .replace("{max}", "" + Game.getSettings().getMaxPlayers()));
        } else
            Chat.send(player, PluginText.replaceToArray(
                    Messages.Lobby.WELCOME_MSG_CONFIGURING,
                    "{cmd}", "uhc tutorial config"));
    }
}
