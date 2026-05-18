package org.mcwonderland.uhc.game.state.share;

import org.mcwonderland.uhc.platform.text.PluginText;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public abstract class QuitListener implements Listener {

    @EventHandler
    public final void onQuit(PlayerQuitEvent e) {
        onPlayerQuit(e);
        e.quitMessage(colorize(e.quitMessage()));
    }

    protected abstract void onPlayerQuit(PlayerQuitEvent e);

    private Component colorize(Component message) {
        if (message == null)
            return null;

        String legacyMessage = PluginText.toLegacyString(message);
        return PluginText.toComponent(legacyMessage);
    }

}
