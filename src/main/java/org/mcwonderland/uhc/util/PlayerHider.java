package org.mcwonderland.uhc.util;

import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.bukkit.entity.Player;

public class PlayerHider {

    public static void hide(Player player, Player target) {
        if (player.canSee(target)) {
            if (LegacyFoundationAdapter.isAtLeastMinecraft1_13())
                player.hidePlayer(WonderlandUHC.getInstance(), target);
            else
                player.hidePlayer(target);
        }
    }

    public static void show(Player player, Player target) {
        if (!player.canSee(target))
            if (LegacyFoundationAdapter.isAtLeastMinecraft1_13())
                player.showPlayer(WonderlandUHC.getInstance(), target);
            else
                player.showPlayer(target);
    }

    public static void hidePlayer(Player target) {
        LegacyFoundationAdapter.getOnlinePlayers().forEach(p -> hide(p, target));
    }

    public static void showPlayer(Player target) {
        LegacyFoundationAdapter.getOnlinePlayers().forEach(p -> show(p, target));
    }

    public static void hideAll(Player target) {
        LegacyFoundationAdapter.getOnlinePlayers().forEach(p -> {
            hide(p, target);
            hide(target, p);
        });
    }

    public static void showAll(Player target) {
        LegacyFoundationAdapter.getOnlinePlayers().forEach(p -> {
            show(p, target);
            show(target, p);
        });
    }



}
