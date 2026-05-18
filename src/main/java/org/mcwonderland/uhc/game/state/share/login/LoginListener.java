package org.mcwonderland.uhc.game.state.share.login;

import org.mcwonderland.uhc.game.state.share.login.checker.LoginChecker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class LoginListener implements Listener {

    private List<LoginChecker> checkers;

    public LoginListener(LoginChecker... checkers) {
        this.checkers = new ArrayList<>(Arrays.asList(checkers));
    }

    @EventHandler
    public final void onLogin(PlayerLoginEvent e) {
        UHCLoginEvent loginEvent = new UHCLoginEvent(e);

        boolean pass = true;

        for (LoginChecker checker : checkers) {
            checker.check(loginEvent);

            if (!loginEvent.isAllowed()) {
                pass = false;

                break;
            }
        }

        if (pass)
            onPassAllChecks(loginEvent);
    }

    protected void onPassAllChecks(UHCLoginEvent e) {

    }
}
