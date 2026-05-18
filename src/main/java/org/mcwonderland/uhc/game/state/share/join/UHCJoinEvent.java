package org.mcwonderland.uhc.game.state.share.join;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.jetbrains.annotations.Nullable;
import org.mcwonderland.uhc.platform.text.PluginText;

public class UHCJoinEvent {

    private final PlayerJoinEvent source;
    @Getter
    private final UHCPlayer uhcPlayer;
    @Getter
    private final Game game;

    public UHCJoinEvent(PlayerJoinEvent source) {
        this.source = source;
        this.uhcPlayer = UHCPlayer.getUHCPlayer(source.getPlayer());
        this.game = Game.getGame();
    }

    public Player getPlayer() {
        return source.getPlayer();
    }

    public void setJoinMessage(@Nullable String joinMessage) {
        source.joinMessage(PluginText.toNullableComponent(joinMessage));
    }
}
