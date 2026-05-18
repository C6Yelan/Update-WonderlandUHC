package org.mcwonderland.uhc.platform.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.mcwonderland.uhc.platform.text.PluginText;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PluginPlayers {
    private PluginPlayers() {
    }

    public static Collection<? extends Player> onlinePlayers() {
        return Bukkit.getOnlinePlayers();
    }

    public static Player getByUniqueId(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.isOnline() ? player : null;
    }

    public static Player getByName(String name, boolean ignoreVanished) {
        Player found = findOnlinePlayer(name);

        if (ignoreVanished && found != null && isVanished(found))
            return null;

        return found;
    }

    public static List<String> playerNames() {
        return onlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    public static void kick(Player player, String... message) {
        PluginScheduler.runLater(0, () -> player.kick(PluginText.toComponent(String.join("\n", message))));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(PluginText.toComponent(message));
    }

    private static Player findOnlinePlayer(String name) {
        Player found = null;
        int delta = Integer.MAX_VALUE;

        for (Player player : onlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name))
                return player;

            if (player.getName().toLowerCase().startsWith(name.toLowerCase())) {
                int currentDelta = Math.abs(player.getName().length() - name.length());

                if (currentDelta < delta) {
                    found = player;
                    delta = currentDelta;
                }
            }
        }

        return found;
    }

    private static boolean isVanished(Player player) {
        return player.hasMetadata("vanished");
    }
}
