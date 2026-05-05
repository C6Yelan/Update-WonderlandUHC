package org.mcwonderland.uhc.platform.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.mcwonderland.uhc.port.SchedulerPort;

public final class PaperSchedulerPort implements SchedulerPort {

    private final Plugin plugin;

    public PaperSchedulerPort(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runLater(long delayTicks, Runnable task) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
}
