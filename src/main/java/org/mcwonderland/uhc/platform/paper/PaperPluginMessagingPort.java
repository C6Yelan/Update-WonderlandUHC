package org.mcwonderland.uhc.platform.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.mcwonderland.uhc.port.PluginMessagingPort;

public final class PaperPluginMessagingPort implements PluginMessagingPort {

    private final Plugin plugin;

    public PaperPluginMessagingPort(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerOutgoingChannel(String channel) {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel);
    }
}
