package org.mcwonderland.uhc.application.world;

import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.util.Extra;

import javax.annotation.Nullable;

public final class PreviewWorldSelectionService {

    public void select(@Nullable Player host) {
        if (WorldLoadingCacheState.getLoadingStatus() == LoadingStatus.DONE)
            return;

        saveCaches(host);
        kickPlayers();
        PluginScheduler.runLater(1, Extra::restartServer);
    }

    private void saveCaches(@Nullable Player host) {
        WorldLoadingCacheState.setLoadingStatus(LoadingStatus.GENERATING);

        if (host != null)
            WorldLoadingCacheState.setHost(host.getName());

        WorldLoadingCacheState.saveCache();
    }

    private void kickPlayers() {
        String message = PluginText.replaceToString(CommandSettings.Uhc.Choose.KICK_INIT_MSG);

        for (Player player : PluginPlayers.onlinePlayers())
            PluginPlayers.kick(player, message);
    }
}
