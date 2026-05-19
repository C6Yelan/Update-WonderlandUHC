package org.mcwonderland.uhc.game.state.share.login.checker;

import org.mcwonderland.uhc.game.state.share.login.UHCLoginEvent;

public abstract class LoginChecker {

    private UHCLoginEvent event;

    public void check(UHCLoginEvent e) {
        this.event = e;
        checkLogin(e);
    }

    protected abstract void checkLogin(UHCLoginEvent e);

    protected void disallow(String message) {
        event.disallow(message);
    }
}
