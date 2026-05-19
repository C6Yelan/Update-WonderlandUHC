package org.mcwonderland.uhc.game.player.role.models;

import org.bukkit.entity.Player;
import org.mcwonderland.uhc.platform.text.PluginText;

public abstract class RoleChat {

    public abstract void chat(Player player, String message);

    protected String replace(String model, Player player, String message) {
        return PluginText.replaceToString(
                model,
                "{player}", player.getName(),
                "{msg}", message
        );
    }

}
