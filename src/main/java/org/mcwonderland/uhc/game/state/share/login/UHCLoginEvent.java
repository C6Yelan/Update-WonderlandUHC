package org.mcwonderland.uhc.game.state.share.login;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

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

    public String getHostname() {
        return source.getHostname();
    }

    public InetAddress getAddress() {
        return source.getAddress();
    }

    public InetAddress getRealAddress() {
        return source.getRealAddress();
    }

    public PlayerLoginEvent.Result getResult() {
        return source.getResult();
    }

    public void setResult(@NotNull PlayerLoginEvent.Result result) {
        source.setResult(result);
    }

    public void setKickMessage(@NotNull String message) {
        source.kickMessage(toComponent(message));
    }

    public void disallow(@NotNull PlayerLoginEvent.Result result, @NotNull String message) {
        source.disallow(result, toComponent(message));
    }

    public void allow() {
        source.allow();
    }

    private Component toComponent(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }
}
