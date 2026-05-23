package org.mcwonderland.uhc.game.state.preparing;

import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.game.state.share.CommonListener;
import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.mcwonderland.uhc.practice.Practice;
import org.mcwonderland.uhc.util.GameUtils;
import org.mcwonderland.uhc.util.Lobby;
import org.mcwonderland.uhc.util.PlayerUtils;
import org.mcwonderland.uhc.util.UHCWorldUtils;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PreparingCommonListener extends CommonListener {

    private WonderlandUHC plugin = WonderlandUHC.getInstance();
    private Practice practice = plugin.getPractice();

    @Override
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = ( Player ) e.getEntity();

            if (!practice.isInPractice(player)) {
                cancelLobbyPlayerDamage(e);
                return;
            }

            if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                super.onDamage(e);
                return;
            }

            if (e instanceof EntityDamageByEntityEvent) {
                Entity damager = (( EntityDamageByEntityEvent ) e).getDamager();

                if (damager instanceof Player) {
                    Player playerDamager = ( Player ) damager;

                    if (practice.isInPractice(playerDamager))
                        return;
                }
            }
        } else
            super.onDamage(e);
    }

    private void cancelLobbyPlayerDamage(EntityDamageEvent e) {
        if (isPlayerCausedDamage(e))
            e.setCancelled(true);
    }

    private boolean isPlayerCausedDamage(EntityDamageEvent e) {
        return getPlayerDamager(e) != null;
    }

    private Player getPlayerDamager(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent))
            return null;

        Entity damager = (( EntityDamageByEntityEvent ) e).getDamager();

        if (damager instanceof Player)
            return ( Player ) damager;

        if (damager instanceof Projectile)
            return PlayerUtils.getShooter(( Projectile ) damager);

        return null;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();

        if (practice.isInPractice(player))
            return;

        e.getDrops().clear();
        e.setDroppedExp(0);
        e.deathMessage(null);

        respawnInLobby(player);
    }

    private void respawnInLobby(Player player) {
        PluginScheduler.runLater(1, () -> {
            if (!shouldHandleLobbyPlayer(player))
                return;

            PlayerUtils.respawnIfDead(player);

            PluginScheduler.runLater(1, () -> {
                if (shouldHandleLobbyPlayer(player))
                    Lobby.send(player);
            });
        });
    }

    private boolean shouldHandleLobbyPlayer(Player player) {
        return player.isOnline() && !practice.isInPractice(player) && GameUtils.isWaiting();
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL && isSeparateLobbyWorld(e.getLocation().getWorld()))
            e.setCancelled(true);
    }

    private boolean isSeparateLobbyWorld(World world) {
        World lobby = UHCWorldUtils.getLobby();

        return world != null && world.equals(lobby) && !UHCWorldUtils.isUhcWorld(world);
    }


    @Override
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();

        if (!practice.isInPractice(player))
            super.onBlockBreak(e);
    }


}
