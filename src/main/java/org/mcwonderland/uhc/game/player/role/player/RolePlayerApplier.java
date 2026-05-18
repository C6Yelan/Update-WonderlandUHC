package org.mcwonderland.uhc.game.player.role.player;

import org.mcwonderland.uhc.game.player.role.models.RoleApplier;
import org.mcwonderland.uhc.util.GameUtils;
import org.mcwonderland.uhc.util.Lobby;
import org.bukkit.entity.Player;

public class RolePlayerApplier implements RoleApplier {

    @Override
    public void apply(Player player) {
        player.setExpCooldown(0);

        if (GameUtils.isWaiting())
            Lobby.stuff(player);
    }

}
