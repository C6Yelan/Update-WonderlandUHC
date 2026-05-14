package org.mcwonderland.uhc.application.world;

import org.bukkit.entity.Player;
import org.mcwonderland.uhc.game.settings.CacheSaver;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.util.Extra;

import javax.annotation.Nullable;

public final class PreviewWorldSelectionService {

    public void select(@Nullable Player host) {
        if (CacheSaver.getLoadingStatus() == LoadingStatus.DONE)
            return;

        saveCaches(host);
        kickPlayers();
        LegacyFoundationAdapter.runLater(1, Extra::restartServer);
    }

    private void saveCaches(@Nullable Player host) {
        CacheSaver.setLoadingStatus(LoadingStatus.GENERATING);

        if (host != null)
            CacheSaver.setHost(host.getName());

        CacheSaver.saveCache();
    }

    private void kickPlayers() {
        String message = LegacyFoundationAdapter.replaceToString(CommandSettings.Uhc.Choose.KICK_INIT_MSG);

        for (Player player : LegacyFoundationAdapter.getOnlinePlayers())
            LegacyFoundationAdapter.kickPlayer(player, message);
    }
}
