package org.mcwonderland.uhc.command.impl.host;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.mcwonderland.uhc.UHCPermission;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.api.event.player.UHCPlayerRespawnedEvent;
import org.mcwonderland.uhc.game.Game;
import org.mcwonderland.uhc.game.player.DeathPlayer;
import org.mcwonderland.uhc.game.player.UHCPlayer;
import org.mcwonderland.uhc.game.settings.sub.UHCItemSettings;
import org.mcwonderland.uhc.model.InventoryContent;
import org.mcwonderland.uhc.model.InvinciblePlayer;
import org.mcwonderland.uhc.model.Teleporter;
import org.mcwonderland.uhc.platform.event.PluginEvents;
import org.mcwonderland.uhc.platform.player.PluginPlayers;
import org.mcwonderland.uhc.platform.scheduler.PluginScheduler;
import org.mcwonderland.uhc.settings.CommandSettings;
import org.mcwonderland.uhc.settings.Messages;
import org.mcwonderland.uhc.settings.Settings;
import org.mcwonderland.uhc.settings.Sounds;
import org.mcwonderland.uhc.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 2019-11-27 下午 01:05
 */
public class RespawnCommand implements CommandExecutor, TabCompleter {

    public static final String NAME = "respawn";

    private static final String PLAYER_NOT_ONLINE = "<red>Player {player} is not online on this server.</red>";

    public static void register(WonderlandUHC plugin) {
        PluginCommand command = plugin.getCommand(NAME);

        if (command == null)
            throw new IllegalStateException("Command /" + NAME + " is not declared in plugin.yml");

        RespawnCommand executor = new RespawnCommand();
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player moderator)) {
            Chat.send(sender, CommandSettings.NO_CONSOLE);
            return true;
        }

        if (!UHCPermission.COMMAND_RESPAWN.checkPerms(moderator))
            return true;

        if (args.length < 1)
            return false;

        Player target = PluginPlayers.getByName(args[0], true);

        if (target == null) {
            Chat.send(moderator, PLAYER_NOT_ONLINE.replace("{player}", args[0]));
            return true;
        }

        if (!GameUtils.isGameStarted()) {
            Chat.send(moderator, Messages.NOT_YET_STARTED);
            return true;
        }

        if (GameUtils.isGamingPlayer(target)) {
            Chat.send(moderator, CommandSettings.Respawn.IS_PLAYING);
            return true;
        }

        new RespawnHandler(moderator, target).respawn();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1)
            return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        for (String name : PluginPlayers.playerNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix))
                completions.add(name);
        }

        return completions;
    }

    class RespawnHandler {

        private final Player moderator;
        private final Player target;
        private final UHCPlayer targetUHCPlayer;
        private Location toTeleport;
        private InventoryContent inv;
        private int level = 0;
        private float exp = 0;

        public RespawnHandler(Player moderator, Player target) {
            this.moderator = moderator;
            this.target = target;
            this.targetUHCPlayer = UHCPlayer.getUHCPlayer(target);
        }

        private void respawn() {
            handleRespawnVaribles();
            handleData();
            if (PlayerUtils.respawnIfDead(target)) {
                PluginScheduler.runLater(1, this::completeRespawn);
                return;
            }

            completeRespawn();
        }

        private void completeRespawn() {
            restoreAndTeleport();
            InvinciblePlayer.addInvincible(targetUHCPlayer, Settings.Game.RESPAWN_INVINCIBLE_TIME);

            Chat.send(target, CommandSettings.Respawn.RESPAWNED);
            Chat.broadcast(CommandSettings.Respawn.BROADCAST
                    .replace("{mod}", moderator.getName())
                    .replace("{player}", target.getName()));
            Extra.sound(target, Sounds.Commands.RESPAWN);

            PluginEvents.callEvent(new UHCPlayerRespawnedEvent(targetUHCPlayer));
        }

        private void restoreAndTeleport() {
            Extra.comepleteClear(target);
            target.setGameMode(GameMode.SURVIVAL);
            inv.setContents(target);
            target.setLevel(level);
            target.setExp(exp);
            target.setExpCooldown(0);
            target.setCollidable(true);
            target.teleport(toTeleport);
        }

        private void handleRespawnVaribles() {
            Game game = Game.getGame();
            DeathPlayer deathPlayer = DeathPlayer.getDeathPlayer(targetUHCPlayer);

            if (deathPlayer != null) {
                inv = deathPlayer.getInvContent();
                level = deathPlayer.getLevel();
                exp = deathPlayer.getExp();
                toTeleport = getRespawnLocation(deathPlayer, game.getCurrentBorder());
            } else {
                UHCItemSettings itemSettings = Game.getSettings().getItemSettings();
                inv = itemSettings.getCustomInventoryItems();
                toTeleport = Teleporter.getRandomTp(UHCWorldUtils.getWorld(), game.getCurrentBorder());
            }
        }

        private Location getRespawnLocation(DeathPlayer dp, int border) {
            /*
            如果在地獄，要判斷兩個點
            1. 地獄是否關閉了
            2. 如果地獄沒關，而且地獄的關閉方式是邊界慢慢縮，則得判斷玩家座標是不是已經在邊界外了
             */

            Location toTeleport;
            if (dp.getLocation().getWorld() == UHCWorldUtils.getNether()
                    && (!GameUtils.isNetherOn() || (Settings.Border.INCLUDE_18_BORDER
                    && !BorderUtil.isInBorder(dp.getLocation(), BorderUtil.getMoveBorder(UHCWorldUtils.getNether()))))) {
                toTeleport = Teleporter.getRandomTp(UHCWorldUtils.getWorld(), border);
            } else if (!BorderUtil.isInBorder(dp.getLocation(), border)) {
                toTeleport = Teleporter.getRandomTp(UHCWorldUtils.getWorld(), border);
            } else {
                toTeleport = dp.getLocation();
            }
            return toTeleport;
        }

        private void handleData() {
            Game.getGame().getWhiteList().add(target);
            targetUHCPlayer.changePlayerRole();
        }
    }
}
