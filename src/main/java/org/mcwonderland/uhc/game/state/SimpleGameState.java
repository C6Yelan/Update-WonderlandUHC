package org.mcwonderland.uhc.game.state;

import lombok.Getter;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.StateName;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.platform.event.PluginEvents;
import org.mcwonderland.uhc.scoreboard.SidebarTheme;
import org.mcwonderland.uhc.scoreboard.line.ScoreLines;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.List;

public abstract class SimpleGameState implements GameState {

    @Getter
    private StateName name;
    private Collection<Listener> listeners;

    public SimpleGameState(StateName name) {
        this.name = name;
    }

    @Override
    public final void init() {
        this.listeners = initListeners();
        this.listeners.forEach(PluginEvents::registerEvents);
        onInit();
    }

    @Override
    public final void start() {
        onStart();
    }

    @Override
    public final void end() {
        this.listeners.forEach(HandlerList::unregisterAll);
        onEnd();
    }

    @Override
    public final List<String> getScoreboard(Player player) {
        UHCPlayer uhcPlayer = UHCPlayer.getUHCPlayer(player);
        SidebarTheme theme = Game.getSettings().getScoreboardSettings().getSidebarTheme();
        return getScoreLines(theme, uhcPlayer).getFor(uhcPlayer);
    }

    protected abstract ScoreLines getScoreLines(SidebarTheme theme, UHCPlayer uhcPlayer);

    protected void onInit() {

    }

    protected void onStart() {

    }

    protected void onEnd() {

    }

    protected abstract Collection<Listener> initListeners();
}
