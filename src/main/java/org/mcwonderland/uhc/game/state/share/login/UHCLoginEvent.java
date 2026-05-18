package org.mcwonderland.uhc.game.state.share.login;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.application.login.LoginPermissionService;
import org.mcwonderland.uhc.application.login.LoginSubject;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;

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
        source.kickMessage(toComponent(message));
    }

    public static Component toComponent(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }
}
