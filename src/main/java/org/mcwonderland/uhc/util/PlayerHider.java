package org.mcwonderland.uhc.util;

import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.WonderlandUHC;
import org.bukkit.entity.Player;

public class PlayerHider {

    public static void hide(Player player, Player target) {
        if (player.canSee(target)) {
            player.hidePlayer(WonderlandUHC.getInstance(), target);
        }
    }

    public static void show(Player player, Player target) {
        if (!player.canSee(target))
            player.showPlayer(WonderlandUHC.getInstance(), target);
    }

    public static void hidePlayer(Player target) {
        PluginPlayers.onlinePlayers().forEach(p -> hide(p, target));
    }

    public static void showPlayer(Player target) {
        PluginPlayers.onlinePlayers().forEach(p -> show(p, target));
    }

    public static void hideAll(Player target) {
        PluginPlayers.onlinePlayers().forEach(p -> {
            hide(p, target);
            hide(target, p);
        });
    }

    public static void showAll(Player target) {
        PluginPlayers.onlinePlayers().forEach(p -> {
            show(p, target);
            show(target, p);
        });
    }



}
