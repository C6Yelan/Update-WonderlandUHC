package org.mcwonderland.uhc.game.player.role.player;

import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.mcwonderland.uhc.game.CombatRelog;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.GameManager;
import org.mcwonderland.uhc.game.UHCTeam;
import org.mcwonderland.uhc.game.player.DeathPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.player.role.models.RoleEventHandler;
import org.mcwonderland.uhc.game.state.share.join.UHCJoinEvent;
import org.mcwonderland.uhc.model.InvinciblePlayer;
import org.mcwonderland.uhc.model.Teleporter;
import org.mcwonderland.uhc.game.timer.Timers;
import org.mcwonderland.uhc.platform.text.PluginText;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.util.BorderUtil;
import org.mcwonderland.uhc.util.Chat;
import org.mcwonderland.uhc.util.Extra;
import org.mcwonderland.uhc.util.GameUtils;
import org.mcwonderland.uhc.util.UHCWorldUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

public class RolePlayerEvents implements RoleEventHandler {

    @Override
    public void onGamingJoin(UHCJoinEvent e) {
        removeRelogAndRestorePlayer(e.getUhcPlayer());
        e.getPlayer().setGameMode(GameMode.SURVIVAL);
    }

    @Override
    public void onStartingJoin(UHCJoinEvent e) {
        UHCPlayer uhcPlayer = e.getUhcPlayer();
        UHCTeam team = uhcPlayer.getTeam();

        boolean firstJoin = (team == null);

        if (firstJoin) {
            team = UHCTeam.createTeamIfNotExist(uhcPlayer);
            Timers.SCATTER.scatter(team);
        } else {
            Player player = uhcPlayer.getPlayer();
            Location location = Timers.SCATTER.getScatterLocation(team);

            if (location == null) {
                player.teleport(UHCWorldUtils.getLobbySpawn());
                BorderUtil.resetLobbyBorderIfSeparate();
            } else {
                player.teleport(location);
                GameManager.freeze(player);
            }
        }
    }

    @Override
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UHCPlayer uhcPlayer = UHCPlayer.getUHCPlayer(player);

        if (isNoRelog())
            player.setHealth(0);
        else {
            Integer relogInMinutes = Settings.CombatRelog.RELOG_IN_MINUTES;

            CombatRelog.setCombatRelog(uhcPlayer);

            e.quitMessage(PluginText.toComponent(PluginText.replaceToString(
                    Messages.Game.PLAYER_DISCONNECT,
                    "{player}", uhcPlayer.getTeam().getChatFormat() + uhcPlayer.getName(),
                    "{time}", relogInMinutes
            )));
        }
    }

    private boolean isNoRelog() {
        return !Settings.CombatRelog.ENABLED
                || (Game.getGame().isPvpEnabled() && !Settings.CombatRelog.RELOG_AFTER_PVP_ENABLED);
    }

    private void removeRelogAndRestorePlayer(UHCPlayer uhcPlayer) {
        Player player = uhcPlayer.getPlayer();

        CombatRelog z = CombatRelog.get(uhcPlayer);
        if (z == null) {
            restorePendingRespawn(uhcPlayer);
            return;
        }

        LivingEntity relogEntity = z.getEntity();
        Extra.copyHealth(relogEntity, player);
        z.getInventoryContent().setContents(player);
        player.setLevel(z.getLevel());
        player.setExp(z.getXp());
        player.addPotionEffects(z.getEntity().getActivePotionEffects());

        PluginScheduler.runLater(1, () -> {
            if (!uhcPlayer.isOnline())
                return;

            relogEntity.remove();
            player.teleport(relogEntity.getLocation());
            z.remove();
        });
    }

    private void restorePendingRespawn(UHCPlayer uhcPlayer) {
        DeathPlayer deathPlayer = DeathPlayer.getDeathPlayer(uhcPlayer);

        if (deathPlayer == null)
            return;

        Player player = uhcPlayer.getPlayer();

        Extra.comepleteClear(player);
        player.setGameMode(GameMode.SURVIVAL);
        deathPlayer.getInvContent().setContents(player);
        player.setLevel(deathPlayer.getLevel());
        player.setExp(deathPlayer.getExp());
        player.setExpCooldown(0);
        player.setCollidable(true);
        player.teleport(getRespawnLocation(deathPlayer, Game.getGame().getCurrentBorder()));
        DeathPlayer.removeDeathPlayer(uhcPlayer);
        InvinciblePlayer.addInvincible(uhcPlayer, Settings.Game.RESPAWN_INVINCIBLE_TIME);
        Chat.send(player, CommandSettings.Respawn.RESPAWNED);
    }

    private Location getRespawnLocation(DeathPlayer deathPlayer, int border) {
        Location deathLocation = deathPlayer.getLocation();

        if (deathLocation.getWorld() == UHCWorldUtils.getNether()
                && (!GameUtils.isNetherOn() || (Settings.Border.INCLUDE_18_BORDER
                && !BorderUtil.isInBorder(deathLocation, BorderUtil.getMoveBorder(UHCWorldUtils.getNether()))))) {
            return Teleporter.getRandomTp(UHCWorldUtils.getWorld(), border);
        }

        if (!BorderUtil.isInBorder(deathLocation, border))
            return Teleporter.getRandomTp(UHCWorldUtils.getWorld(), border);

        return deathLocation;
    }
}
