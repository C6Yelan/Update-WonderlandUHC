package org.mcwonderland.uhc.game.player.role.spectator;

import org.mcwonderland.uhc.game.player.role.models.RoleApplier;
import org.mcwonderland.uhc.legacy.LegacyFoundationAdapter;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.tools.Hotbars;
import org.mcwonderland.uhc.util.Extra;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class RoleSpectatorApplier implements RoleApplier {

    @Override
    public void apply(Player player) {
        Extra.comepleteClear(player);
        player.setExpCooldown(Integer.MAX_VALUE);
        player.setCollidable(false);

        switch (Settings.Spectator.SPECTATE_MODE) {
            case SPECTATOR: //
                player.setGameMode(GameMode.SPECTATOR);
                break;
            case DEFAULT:
                player.setGameMode(GameMode.CREATIVE);
                LegacyFoundationAdapter.runLater(0, () -> Hotbars.giveSpecItems(player));
                break;
        }
    }

}
