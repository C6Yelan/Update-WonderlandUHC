package org.mcwonderland.uhc;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@Getter
public enum Dependency {
    LUCK_PERMS("LuckPerms", "https://modrinth.com/plugin/luckperms/changelog?g=1.21.11&l=paper"),
    DISCORD_SRV("DiscordSRV", "https://modrinth.com/plugin/discordsrv/versions?g=1.21.11&l=paper"),
    CHUNKY("Chunky", "https://modrinth.com/plugin/chunky/changelog?c=release&g=1.21.11&l=paper");

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

    public String getClickableDownloadUrlTag() {
        return "<click:open_url:'" + downloadUrl + "'>" + downloadUrl + "</click>";
    }
}
