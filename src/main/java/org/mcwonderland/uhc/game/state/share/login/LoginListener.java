package org.mcwonderland.uhc.game.state.share.login;

import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.mcwonderland.uhc.application.login.LoginSubject;
import org.mcwonderland.uhc.game.state.share.login.checker.LoginChecker;
import org.mcwonderland.uhc.integration.luckperms.LuckPermsLoginPermissionService;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.Messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public abstract class LoginListener implements Listener {
    private static final LuckPermsLoginPermissionService LOGIN_PERMISSIONS = new LuckPermsLoginPermissionService();

    private List<LoginChecker> checkers;

    public LoginListener(LoginChecker... checkers) {
        this.checkers = new ArrayList<>(Arrays.asList(checkers));
    }

    @EventHandler
    public final void onLogin(PlayerConnectionValidateLoginEvent e) {
        if (!e.isAllowed())
            return;

        Optional<LoginSubject> subject = LoginSubject.from(e.getConnection());
        if (subject.isEmpty()) {
            e.kickMessage(PluginText.toComponent(Messages.Kick.WAITING_HOST));
            return;
        }

        UHCLoginEvent loginEvent = new UHCLoginEvent(e, subject.get(), LOGIN_PERMISSIONS);

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
