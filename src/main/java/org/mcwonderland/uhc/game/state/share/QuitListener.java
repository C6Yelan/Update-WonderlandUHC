package org.mcwonderland.uhc.game.state.share;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
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

        String legacyMessage = LegacyComponentSerializer.legacySection().serialize(message);
        return LegacyComponentSerializer.legacySection().deserialize(LegacyFoundationAdapter.colorize(legacyMessage));
    }

}
