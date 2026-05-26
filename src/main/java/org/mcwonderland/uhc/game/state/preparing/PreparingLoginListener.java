package org.mcwonderland.uhc.game.state.preparing;

import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.state.share.login.LoginListener;
import org.mcwonderland.uhc.game.state.share.login.UHCLoginEvent;
import org.mcwonderland.uhc.game.state.share.login.checker.LoginChecker;
import org.mcwonderland.uhc.game.state.share.login.checker.WhitelistChecker;
import org.mcwonderland.uhc.settings.Messages;

public class PreparingLoginListener extends LoginListener {

    public PreparingLoginListener() {
        super(
                new GeneratingChecker(),
                new ConfigChecker(),
                new WhitelistChecker(),
                new FullChecker()
        );
    }

    private static class GeneratingChecker extends LoginChecker {
        @Override
        protected void checkLogin(UHCLoginEvent e) {
            if (WorldLoadingCacheState.getLoadingStatus() == LoadingStatus.GENERATING)
                disallow(Messages.Kick.GENERATING);
        }
    }

    private static class ConfigChecker extends LoginChecker {

        @Override
        protected void checkLogin(UHCLoginEvent e) {
            LoadingStatus loadingStatus = WorldLoadingCacheState.getLoadingStatus();

            if (loadingStatus.isWaitingForHost()
                    && !e.hasPermission(UHCPermission.BYPASS_JOIN_CONFIGURING))
                disallow(Messages.Kick.WAITING_HOST);
        }
    }

    private static class FullChecker extends LoginChecker {

        @Override
        protected void checkLogin(UHCLoginEvent e) {

            if (WorldLoadingCacheState.getLoadingStatus() == LoadingStatus.DONE) {
                boolean full = PluginPlayers.onlinePlayers().size() >= Game.getSettings().getMaxPlayers();

                if (full && !e.hasPermission(UHCPermission.BYPASS_JOIN_FULL))
                    disallow(Messages.Kick.FULL);
            }
        }
    }
}
