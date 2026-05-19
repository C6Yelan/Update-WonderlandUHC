package org.mcwonderland.uhc.game.state.preparing;

import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.state.share.MotdListener;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.util.Extra;

public class PreparingMotdListener extends MotdListener {

    @Override
    protected String getMotd() {
        LoadingStatus loadingStatus = WorldLoadingCacheState.getLoadingStatus();

        if (loadingStatus == LoadingStatus.DONE)
            return PluginText.replaceToString(
                    Messages.Motd.WAITING,
                    "{online}", Extra.getOnlinePlayers(),
                    "{max}", Game.getSettings().getMaxPlayers()
            );

        if (loadingStatus == LoadingStatus.GENERATING)
            return Messages.Motd.GENERATING;

        return Messages.Motd.CONFIGURING;
    }
}
