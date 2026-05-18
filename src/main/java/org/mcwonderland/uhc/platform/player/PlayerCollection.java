package org.mcwonderland.uhc.platform.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashSet;

public class PlayerCollection extends HashSet<String> {

    public boolean add(Player player) {
        if (contains(player))
            return false;

        return super.add(player.getUniqueId().toString());
    }

    @Override
    public boolean add(String playerName) {
        Player player = Bukkit.getPlayer(playerName);

        if (player != null)
            return add(player);

        return super.add(playerName);
    }

    public boolean remove(Player player) {
        return removeAll(Arrays.asList(player.getName(), player.getUniqueId().toString()));
    }

    @Override
    public boolean remove(Object nameOrUuid) {
        if (!(nameOrUuid instanceof String))
            return super.remove(nameOrUuid);

        Player player = Bukkit.getPlayer((String) nameOrUuid);

        if (player != null)
            return remove(player);

        return super.remove(nameOrUuid);
    }

    public boolean contains(Player player) {
        return stream().anyMatch(entry -> entry.equalsIgnoreCase(player.getName())
                || entry.equalsIgnoreCase(player.getUniqueId().toString()));
    }

    public boolean contains(String nameOrUuid) {
        Player player = Bukkit.getPlayer(nameOrUuid);

        if (player != null)
            return contains(player);

        return stream().anyMatch(entry -> entry.equalsIgnoreCase(nameOrUuid));
    }
}
