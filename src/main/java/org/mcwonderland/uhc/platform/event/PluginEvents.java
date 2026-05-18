package org.mcwonderland.uhc.platform.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.mcwonderland.uhc.WonderlandUHC;

public final class PluginEvents {

    private PluginEvents() {
    }

    public static boolean callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
        return !(event instanceof Cancellable) || !(( Cancellable ) event).isCancelled();
    }

    public static void registerEvents(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, WonderlandUHC.getInstance());
    }
}
