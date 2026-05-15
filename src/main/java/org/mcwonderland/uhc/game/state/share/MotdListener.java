package org.mcwonderland.uhc.game.state.share;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public abstract class MotdListener implements Listener {

    @EventHandler
    public void onServerListPing(ServerListPingEvent e) {
        if (!Settings.Misc.CHANGE_MOTD)
            return;

        e.motd(toComponent(LegacyFoundationAdapter.colorize(getMotd())));
    }

    protected abstract String getMotd();

    private Component toComponent(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }

}
