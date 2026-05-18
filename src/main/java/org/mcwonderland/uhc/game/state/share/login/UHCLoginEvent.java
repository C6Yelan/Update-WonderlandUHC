package org.mcwonderland.uhc.game.state.share.login;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;

public class UHCLoginEvent {

    private final PlayerLoginEvent source;
    @Getter
    private final UHCPlayer uhcPlayer;
    @Getter
    private final Game game;

    public UHCLoginEvent(PlayerLoginEvent source) {
        this.source = source;
        this.uhcPlayer = UHCPlayer.getUHCPlayer(source.getPlayer());
        this.game = Game.getGame();
    }

    public Player getPlayer() {
        return source.getPlayer();
    }

    public boolean isAllowed() {
        return source.getResult() == PlayerLoginEvent.Result.ALLOWED;
    }

    public void disallow(String message) {
        source.disallow(PlayerLoginEvent.Result.KICK_OTHER, toComponent(message));
    }

    private Component toComponent(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }
}
