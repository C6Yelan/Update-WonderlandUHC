package org.mcwonderland.uhc.game.state.preparing;

import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.settings.LoadingStatus;
import org.mcwonderland.uhc.game.settings.WorldLoadingCacheState;
import org.mcwonderland.uhc.game.state.share.join.ClearBehavior;
import org.mcwonderland.uhc.game.state.share.join.JoinListener;
import org.mcwonderland.uhc.game.state.share.join.UHCJoinEvent;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.tools.Hotbars;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.BorderUtil;
import org.mcwonderland.uhc.util.PlayerHider;
import org.mcwonderland.uhc.util.UHCWorldUtils;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.entity.Player;

public class PreparingJoinListener extends JoinListener {

    public PreparingJoinListener() {
        super(
                new ClearBehavior()
        );
    }

    @Override
    protected void onPlayerJoin(UHCJoinEvent e) {
        Player player = e.getPlayer();
        Game game = e.getGame();

        player.teleport(UHCWorldUtils.getLobbySpawn());
        player.getWorld().setGameRule(GameRules.LOCATOR_BAR, false);
        BorderUtil.resetLobbyBorderIfSeparate();

        Hotbars.giveLobbyItems(player);

        player.setGameMode(GameMode.ADVENTURE);

        if (Settings.Misc.LOBBY_HIDE)
            PlayerHider.hideAll(player);

        if (game.getHost().isEmpty())
            game.setHost(player.getName());

        LoadingStatus loadingStatus = WorldLoadingCacheState.getLoadingStatus();
        if (loadingStatus.isWaitingForHost() && player.getName().equalsIgnoreCase(game.getHost())) {
            Chat.send(player, "<green>歡迎主持人 </green><white>" + player.getName() + "</white><green>。</green>");
            Chat.send(player, PluginText.replaceToArray(
                    Messages.Lobby.WELCOME_MSG_CONFIGURING,
                    "{cmd}", "uhc tutorial config"));
        }
    }
}
