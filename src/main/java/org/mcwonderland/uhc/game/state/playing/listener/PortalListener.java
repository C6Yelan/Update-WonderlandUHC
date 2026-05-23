package org.mcwonderland.uhc.game.state.playing.listener;

import org.mcwonderland.uhc.application.world.MatchCenter;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.GameUtils;
import org.mcwonderland.uhc.util.UHCWorldUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PortalListener implements Listener {
    private static final double NETHER_COORDINATE_SCALE = 8D;

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        Player p = e.getPlayer();

        if (!GameUtils.isGamingPlayer(p)) {
            e.setCancelled(true);
            return;
        }

        if (getToLocation(e).getWorld() == UHCWorldUtils.getNether()) {
            if (!Game.getSettings().isUsingNether()) {
                cancelJoin(e, p, Messages.Game.NO_NETHER);
                return;
            }
        }

        Location to = getSameCoordsLocation(e);
        to.getChunk().load();
        e.setTo(to);
    }

    private void cancelJoin(Cancellable cancellable, Player player, String msg) {
        cancellable.setCancelled(true);

        Chat.send(player, msg);
        Extra.sound(player, Sounds.Game.CANT_JOIN_NETHER);
    }

    private Location getSameCoordsLocation(PlayerPortalEvent e) {
        Location to = getToLocation(e);

        World.Environment environment = to.getWorld().getEnvironment();
        MatchCenter overworldCenter = UHCWorldUtils.getBorderCenter(UHCWorldUtils.getWorld(), Game.getGame().getCurrentBorder());
        MatchCenter netherCenter = UHCWorldUtils.getBorderCenter(UHCWorldUtils.getNether(), GameUtils.getCurrentNetherBorder());

        if (environment == World.Environment.NETHER) {
            to.setX((to.getX() - overworldCenter.getX()) / NETHER_COORDINATE_SCALE + netherCenter.getX());
            to.setZ((to.getZ() - overworldCenter.getZ()) / NETHER_COORDINATE_SCALE + netherCenter.getZ());
        } else if (environment == World.Environment.NORMAL) {
            to.setX((to.getX() - netherCenter.getX()) * NETHER_COORDINATE_SCALE + overworldCenter.getX());
            to.setZ((to.getZ() - netherCenter.getZ()) * NETHER_COORDINATE_SCALE + overworldCenter.getZ());
        }

        return to;
    }

    private Location getToLocation(PlayerPortalEvent e) {
        Location from = e.getFrom().clone();
        from.setWorld(e.getFrom().getWorld() == UHCWorldUtils.getWorld()
                ? UHCWorldUtils.getNether()
                : UHCWorldUtils.getWorld());

        return from;
    }
}
