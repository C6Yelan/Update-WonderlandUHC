package org.mcwonderland.uhc;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@Getter
public enum Dependency {
    DISCORD_SRV("DiscordSRV", "https://www.spigotmc.org/resources/discordsrv.18494/"),
    CHUNKY("Chunky", "https://modrinth.com/plugin/chunky");

    private final String pluginName;
    private final String downloadUrl;

    Dependency(String pluginName, String downloadUrl) {
        this.pluginName = pluginName;
        this.downloadUrl = downloadUrl;
    }

    public String getVersion() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

        return plugin == null ? "" : plugin.getPluginMeta().getVersion();
    }

    public boolean isHooked() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

        return plugin != null && plugin.isEnabled();
    }
}
