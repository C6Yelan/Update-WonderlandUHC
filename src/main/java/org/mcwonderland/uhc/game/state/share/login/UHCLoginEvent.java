package org.mcwonderland.uhc.game.state.share.login;

import lombok.Getter;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.application.login.LoginPermissionService;
import org.mcwonderland.uhc.application.login.LoginSubject;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.text.PluginText;

public class UHCLoginEvent {

    private final PlayerConnectionValidateLoginEvent source;
    @Getter
    private final LoginSubject subject;
    private final LoginPermissionService permissions;
    @Getter
    private final UHCPlayer uhcPlayer;
    @Getter
    private final Game game;

    public UHCLoginEvent(PlayerConnectionValidateLoginEvent source, LoginSubject subject, LoginPermissionService permissions) {
        this.source = source;
        this.subject = subject;
        this.permissions = permissions;
        this.uhcPlayer = UHCPlayer.getExisting(subject.getUniqueId(), subject.getName());
        this.game = Game.getGame();
    }

    public boolean isAllowed() {
        return source.isAllowed();
    }

    public boolean hasPermission(UHCPermission permission) {
        return permissions.hasPermission(subject, permission);
    }

    public void disallow(String message) {
        source.kickMessage(PluginText.toComponent(message));
    }
}
