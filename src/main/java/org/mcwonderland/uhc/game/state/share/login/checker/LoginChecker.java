package org.mcwonderland.uhc.game.state.share.login.checker;

import org.mcwonderland.uhc.game.state.share.login.UHCLoginEvent;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.bukkit.event.player.PlayerLoginEvent;

public abstract class LoginChecker {

    private UHCLoginEvent event;

    public void check(UHCLoginEvent e) {
        this.event = e;
        checkLogin(e);
    }

    protected abstract void checkLogin(UHCLoginEvent e);

    protected void disallow(String message) {
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, LegacyFoundationAdapter.colorize(message));
    }
}
