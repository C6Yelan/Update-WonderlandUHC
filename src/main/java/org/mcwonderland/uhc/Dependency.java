package org.mcwonderland.uhc;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.Messages;

@Getter
public enum Dependency {
    DISCORD_SRV("DiscordSRV", "https://www.spigotmc.org/resources/discordsrv.18494/"),
    CHUNKY("Chunky", "https://modrinth.com/plugin/chunky"),
    WORLD_BORDER("WorldBorder", LegacyFoundationAdapter.isAtLeastMinecraft1_13() ? "https://www.spigotmc.org/resources/worldborder.60905" : "https://dev.bukkit.org/projects/worldborder");

    private final String pluginName;
    private final String downloadUrl;

    Dependency(String pluginName, String downloadUrl) {
        this.pluginName = pluginName;
        this.downloadUrl = downloadUrl;
    }

    public void check() {
        checkExist(Messages.Dependency.REQUIRE_DEPENDENCY);
    }

    public void checkSoft() {
        checkExist(Messages.Dependency.REQUIRE_SOFT_DEPENDENCY);
    }

    public String getVersion() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

        return plugin == null ? "" : plugin.getDescription().getVersion();
    }

    private void checkExist(String falseMsg) {
        LegacyFoundationAdapter.checkBoolean(isHooked(),
                falseMsg.replace("{plugin}", pluginName)
                        .replace("{url}", downloadUrl));
    }

    public boolean isHooked() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

        return plugin != null && plugin.isEnabled();
    }
}
