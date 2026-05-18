package org.mcwonderland.uhc.game.state.playing.listener;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.api.enums.RoleName;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.state.share.login.LoginListener;
import org.mcwonderland.uhc.game.state.share.login.UHCLoginEvent;
import org.mcwonderland.uhc.game.state.share.login.checker.LoginChecker;
import org.mcwonderland.uhc.game.state.share.login.checker.WhitelistChecker;
import org.mcwonderland.uhc.settings.Messages;

public class PlayingLoginListener extends LoginListener {

    public PlayingLoginListener() {
        super(
                new WhitelistChecker(),
                new BypassGameStartedChecker()
        );
    }

    private static class BypassGameStartedChecker extends LoginChecker {

        @Override
        protected void checkLogin(UHCLoginEvent e) {
            UHCPlayer uhcPlayer = e.getUhcPlayer();
            boolean gamingPlayer = uhcPlayer != null && uhcPlayer.getRoleName() == RoleName.PLAYER;

            if (!gamingPlayer
                    && !e.hasPermission(UHCPermission.BYPASS_JOIN_STARTED))
                disallow(Messages.Kick.GAME_STARTED);
        }
    }
}
