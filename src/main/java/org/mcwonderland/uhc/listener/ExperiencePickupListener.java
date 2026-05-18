package org.mcwonderland.uhc.listener;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.api.enums.RoleName;
import org.mcwonderland.uhc.game.player.UHCPlayer;

public class ExperiencePickupListener implements Listener {

    private static final int FOLLOW_RANGE = 8;

    @EventHandler
    public void preventNonPlayerExpTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof ExperienceOrb orb))
            return;
        if (!(event.getTarget() instanceof Player target))
            return;
        if (!isNonPlayerRole(target))
            return;

        event.setTarget(findNearestPlayerRole(orb));
    }

    @EventHandler
    public void preventNonPlayerExpPickup(PlayerPickupExperienceEvent event) {
        if (isNonPlayerRole(event.getPlayer()))
            event.setCancelled(true);
    }

    private Player findNearestPlayerRole(ExperienceOrb orb) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : orb.getNearbyEntities(FOLLOW_RANGE, FOLLOW_RANGE, FOLLOW_RANGE)) {
            if (!(entity instanceof Player player))
                continue;
            if (!hasRole(player, RoleName.PLAYER))
                continue;

            double distance = player.getLocation().distanceSquared(orb.getLocation());
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private boolean isNonPlayerRole(Player player) {
        return hasRole(player, RoleName.SPECTATOR) || hasRole(player, RoleName.STAFF);
    }

    private boolean hasRole(Player player, RoleName roleName) {
        UHCPlayer uhcPlayer = UHCPlayer.getFromEntity(player);
        return uhcPlayer != null && uhcPlayer.getRoleName() == roleName;
    }
}
