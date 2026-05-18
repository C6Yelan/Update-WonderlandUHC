package org.mcwonderland.uhc.platform.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.mcwonderland.uhc.WonderlandUHC;

public final class PluginScheduler {

    private PluginScheduler() {
    }

    public static BukkitTask runLater(long delayTicks, Runnable task) {
        Plugin plugin = WonderlandUHC.getInstance();
        if (!plugin.isEnabled()) {
            task.run();
            return null;
        }

        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    public static BukkitTask runAsync(Runnable task) {
        return runLaterAsync(0, task);
    }

    public static BukkitTask runLaterAsync(Runnable task) {
        return runLaterAsync(0, task);
    }

    public static BukkitTask runLaterAsync(long delayTicks, Runnable task) {
        Plugin plugin = WonderlandUHC.getInstance();
        if (!plugin.isEnabled()) {
            task.run();
            return null;
        }

        if (delayTicks == 0)
            return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);

        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    public static BukkitTask runTimer(long repeatTicks, Runnable task) {
        return runTimer(0, repeatTicks, task);
    }

    public static BukkitTask runTimer(long delayTicks, long repeatTicks, Runnable task) {
        Plugin plugin = WonderlandUHC.getInstance();
        if (!plugin.isEnabled()) {
            task.run();
            return null;
        }

        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, repeatTicks);
    }

    public static BukkitTask runTimerAsync(long repeatTicks, Runnable task) {
        Plugin plugin = WonderlandUHC.getInstance();
        if (!plugin.isEnabled()) {
            task.run();
            return null;
        }

        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, 0, repeatTicks);
    }
}
