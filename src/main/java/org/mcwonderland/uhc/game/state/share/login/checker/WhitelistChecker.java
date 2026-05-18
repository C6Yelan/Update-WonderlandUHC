package org.mcwonderland.uhc.game.state.share.login.checker;

import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.state.share.login.UHCLoginEvent;
import org.mcwonderland.uhc.settings.Messages;

public class WhitelistChecker extends LoginChecker {

    @Override
    protected void checkLogin(UHCLoginEvent e) {
        boolean whitelistOn = Game.getSettings().isWhitelistOn();
        boolean whiteListed = e.getGame().getWhiteList().contains(e.getSubject().getUniqueId(), e.getSubject().getName());
        boolean hasPerm = e.hasPermission(UHCPermission.BYPASS_JOIN_WHITELIST);

        if (whitelistOn && (!whiteListed && !hasPerm)) {
            disallow(Messages.Kick.WHITELISTED);
        }
    }
}
